package com.mathbuddy.service;

import com.mathbuddy.dto.*;
import com.mathbuddy.entity.Child;
import com.mathbuddy.entity.QuestionAttempt;
import com.mathbuddy.entity.QuizSession;
import com.mathbuddy.repository.ChildRepository;
import com.mathbuddy.repository.QuestionAttemptRepository;
import com.mathbuddy.repository.QuizSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Core business logic for MathBuddy.
 * Manages quiz lifecycle, child profile association, deterministic answer validation,
 * question-level attempt persistence, and Gemini voice intent execution.
 */
@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final QuizSessionRepository sessionRepo;
    private final QuestionAttemptRepository attemptRepo;
    private final ChildRepository childRepo;
    private final GeminiAiService aiService;

    public QuizService(QuizSessionRepository sessionRepo,
                       QuestionAttemptRepository attemptRepo,
                       ChildRepository childRepo,
                       GeminiAiService aiService) {
        this.sessionRepo = sessionRepo;
        this.attemptRepo = attemptRepo;
        this.childRepo = childRepo;
        this.aiService = aiService;
    }

    // ── Start a new quiz session ──────────────────────────────────────────────

    @Transactional
    public QuizResponse startQuiz(StartQuizRequest req) {
        String topic = sanitize(req.getTopic(), "addition");
        String difficulty = sanitize(req.getDifficulty(), "easy");

        Child child = null;
        if (req.getChildId() != null) {
            child = childRepo.findById(req.getChildId())
                    .orElseThrow(() -> new IllegalArgumentException("Child profile not found: " + req.getChildId()));
        }

        // 1. Persist the session in MySQL associated with Child (if provided)
        QuizSession session = new QuizSession(child, topic, difficulty);
        session = sessionRepo.save(session);
        Long sessionId = session.getId();

        // 2. Ask Gemini for the first question
        Map<String, Object> q = aiService.generateQuestion(topic, difficulty);

        log.info("Started session {} (child={}) - topic={} difficulty={}",
                sessionId, child != null ? child.getName() : "None", topic, difficulty);

        return QuizResponse.success(
                sessionId,
                (String) q.get("question"),
                toDouble(q.get("answer")),
                (String) q.get("explanation"),
                topic,
                difficulty
        );
    }

    // ── Validate an answer (Deterministic Java Comparison + Attempt Persistence)

    @Transactional
    public AnswerResponse checkAnswer(AnswerRequest req) {
        QuizSession session = sessionRepo.findById(req.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + req.getSessionId()));

        // --- Deterministic numeric comparison (never trust the LLM for math correctness) ---
        boolean correct = isAnswerCorrect(req.getUserAnswer(), req.getCorrectAnswer());

        // Update session stats
        session.setQuestionsAttempted(session.getQuestionsAttempted() + 1);
        if (correct) {
            session.setCorrectAnswers(session.getCorrectAnswers() + 1);
            session.setScore(session.getScore() + 10);
        }
        session.recalculateStars();
        session = sessionRepo.save(session);

        // Generate AI feedback via Gemini & Owl's Math Adventure
        String feedback;
        MathExplanationDto explanationData = null;
        if (correct) {
            feedback = aiService.generatePositiveFeedback(req.getQuestion(), req.getCorrectAnswer());
        } else {
            explanationData = aiService.generateStructuredExplanation(
                    req.getQuestion(), req.getUserAnswer(), req.getCorrectAnswer(), session.getTopic());
            feedback = aiService.generateExplanation(
                    req.getQuestion(), req.getUserAnswer(), req.getCorrectAnswer(), session.getTopic());
        }

        // Persist question-level attempt for deep progress tracking & recommendations
        QuestionAttempt attempt = new QuestionAttempt(
                session,
                session.getChild(),
                req.getQuestion(),
                session.getTopic(),
                req.getCorrectAnswer(),
                req.getUserAnswer(),
                correct,
                feedback
        );
        attemptRepo.save(attempt);

        return AnswerResponse.of(
                correct,
                req.getCorrectAnswer(),
                feedback,
                session.getScore(),
                session.getQuestionsAttempted(),
                session.getCorrectAnswers(),
                session.getStarsEarned(),
                explanationData
        );
    }

    // ── Get next question ─────────────────────────────────────────────────────

    @Transactional
    public QuizResponse nextQuestion(Long sessionId, String topic, String difficulty) {
        QuizSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (topic != null && !topic.isBlank()) {
            session.setTopic(sanitize(topic, session.getTopic()));
        }
        if (difficulty != null && !difficulty.isBlank()) {
            session.setDifficulty(sanitize(difficulty, session.getDifficulty()));
        }
        sessionRepo.save(session);

        Map<String, Object> q = aiService.generateQuestion(
                session.getTopic(), session.getDifficulty());

        return QuizResponse.success(
                sessionId,
                (String) q.get("question"),
                toDouble(q.get("answer")),
                (String) q.get("explanation"),
                session.getTopic(),
                session.getDifficulty()
        );
    }

    // ── Complete session ──────────────────────────────────────────────────────

    @Transactional
    public void endQuiz(Long sessionId) {
        sessionRepo.findById(sessionId).ifPresent(session -> {
            session.setStatus("COMPLETED");
            sessionRepo.save(session);
            log.info("Marked quiz session {} as COMPLETED", sessionId);
        });
    }

    // ── Get explanation ───────────────────────────────────────────────────────

    public ExplainResponse explain(ExplainRequest req) {
        MathExplanationDto details = aiService.generateStructuredExplanation(
                req.getQuestion(), req.getUserAnswer(),
                req.getCorrectAnswer(), req.getTopic());
        String explanation = aiService.generateExplanation(
                req.getQuestion(), req.getUserAnswer(),
                req.getCorrectAnswer(), req.getTopic());
        return ExplainResponse.of(explanation, details);
    }

    // ── Intent Understanding & Voice Action Execution ─────────────────────────

    @Transactional
    public VoiceUnderstandResponse processVoiceInput(VoiceUnderstandRequest req) {
        String speech = req.getSpeechText();
        log.info("Processing voice input: '{}'", speech);

        // 1. Ask Gemini to understand natural-language intent and parameters
        Map<String, Object> parsed = aiService.understandVoiceIntent(
                speech, req.getTopic(), req.getDifficulty(), req.getCurrentQuestion());

        String intent = (String) parsed.getOrDefault("intent", "UNKNOWN");
        String topic = (String) parsed.get("topic");
        String difficulty = (String) parsed.get("difficulty");
        String answer = (String) parsed.get("answer");
        String spokenReply = (String) parsed.getOrDefault("spokenReply", "I heard you!");

        VoiceUnderstandResponse.VoiceUnderstandResponseBuilder builder = VoiceUnderstandResponse.builder()
                .intent(intent)
                .topic(topic)
                .difficulty(difficulty)
                .answer(answer)
                .success(true);

        try {
            switch (intent) {
                case "START_QUIZ" -> {
                    String quizTopic = topic != null ? topic : (req.getTopic() != null ? req.getTopic() : "addition");
                    String quizDiff = difficulty != null ? difficulty : (req.getDifficulty() != null ? req.getDifficulty() : "easy");

                    StartQuizRequest sqr = new StartQuizRequest();
                    sqr.setChildId(req.getChildId());
                    sqr.setTopic(quizTopic);
                    sqr.setDifficulty(quizDiff);
                    QuizResponse qr = startQuiz(sqr);

                    builder.action("QUIZ_STARTED")
                            .topic(quizTopic)
                            .difficulty(quizDiff)
                            .quizData(qr)
                            .spokenResponse("Awesome! Let's start! Here is your question: " + qr.getQuestion());
                }

                case "ANSWER_QUESTION" -> {
                    if (req.getSessionId() == null) {
                        builder.action("NO_SESSION")
                                .spokenResponse("Please start a quiz first! You can say: 'Start an easy addition quiz'.");
                        break;
                    }

                    String answerToValidate = (answer != null && !answer.isBlank()) ? answer : speech;
                    AnswerRequest ar = new AnswerRequest();
                    ar.setSessionId(req.getSessionId());
                    ar.setQuestion(req.getCurrentQuestion());
                    ar.setUserAnswer(answerToValidate);
                    ar.setCorrectAnswer(req.getCurrentAnswer() != null ? req.getCurrentAnswer() : 0.0);

                    AnswerResponse ansResp = checkAnswer(ar);

                    builder.action("ANSWER_CHECKED")
                            .answer(answerToValidate)
                            .answerData(ansResp)
                            .spokenResponse(ansResp.getFeedback());
                }

                case "NEXT_QUESTION" -> {
                    if (req.getSessionId() == null) {
                        builder.action("NO_SESSION")
                                .spokenResponse("No quiz is active. Say: 'Start a quiz' to begin!");
                        break;
                    }
                    QuizResponse qr = nextQuestion(req.getSessionId(), topic, difficulty);
                    builder.action("NEXT_QUESTION")
                            .quizData(qr)
                            .spokenResponse("Here is your next question: " + qr.getQuestion());
                }

                case "EXPLAIN" -> {
                    if (req.getCurrentQuestion() == null || req.getCurrentQuestion().isBlank()) {
                        builder.action("NO_QUESTION")
                                .spokenResponse("Start a quiz first so I can explain the math problem!");
                        break;
                    }
                    ExplainRequest er = new ExplainRequest();
                    er.setSessionId(req.getSessionId());
                    er.setQuestion(req.getCurrentQuestion());
                    er.setTopic(req.getTopic());
                    er.setDifficulty(req.getDifficulty());
                    er.setUserAnswer(answer != null ? answer : "?");
                    er.setCorrectAnswer(req.getCurrentAnswer() != null ? req.getCurrentAnswer() : 0.0);

                    ExplainResponse expResp = explain(er);
                    builder.action("EXPLAINED")
                            .explainData(expResp)
                            .spokenResponse(expResp.getExplanation());
                }

                case "REPEAT" -> {
                    if (req.getCurrentQuestion() != null && !req.getCurrentQuestion().isBlank()) {
                        builder.action("REPEATED")
                                .spokenResponse("I'll repeat the question: " + req.getCurrentQuestion());
                    } else {
                        builder.action("NO_QUESTION")
                                .spokenResponse("There is no question to repeat yet. Let's start a quiz!");
                    }
                }

                case "CHANGE_DIFFICULTY" -> {
                    String newDiff = difficulty != null ? difficulty : "easy";
                    if (req.getSessionId() != null) {
                        QuizResponse qr = nextQuestion(req.getSessionId(), null, newDiff);
                        builder.action("DIFFICULTY_CHANGED")
                                .difficulty(newDiff)
                                .quizData(qr)
                                .spokenResponse("Changed difficulty to " + newDiff + "! Here is your question: " + qr.getQuestion());
                    } else {
                        builder.action("DIFFICULTY_SET")
                                .difficulty(newDiff)
                                .spokenResponse("Set difficulty to " + newDiff + ". Say 'Start' when you're ready!");
                    }
                }

                case "CHANGE_TOPIC" -> {
                    String newTopic = topic != null ? topic : "addition";
                    if (req.getSessionId() != null) {
                        QuizResponse qr = nextQuestion(req.getSessionId(), newTopic, null);
                        builder.action("TOPIC_CHANGED")
                                .topic(newTopic)
                                .quizData(qr)
                                .spokenResponse("Switched topic to " + newTopic + "! Here is your question: " + qr.getQuestion());
                    } else {
                        builder.action("TOPIC_SET")
                                .topic(newTopic)
                                .spokenResponse("Switched topic to " + newTopic + ". Say 'Start' to begin!");
                    }
                }

                default -> {
                    builder.action("UNKNOWN_PROMPT")
                            .spokenResponse(spokenReply != null ? spokenReply
                                    : "I heard you, but I wasn't sure what to do. Try saying an answer or 'Next question'!");
                }
            }
        } catch (Exception e) {
            log.error("Error executing voice action for intent {}: {}", intent, e.getMessage(), e);
            builder.action("ERROR")
                    .spokenResponse("Sorry, something went wrong while processing that. Let's try again!");
        }

        return builder.build();
    }

    // ── Progress dashboard ────────────────────────────────────────────────────

    public ProgressResponse getProgress() {
        Long totalAttempted = sessionRepo.sumQuestionsAttempted();
        Long totalCorrect = sessionRepo.sumCorrectAnswers();
        Long totalStars = sessionRepo.sumStarsEarned();

        totalAttempted = (totalAttempted == null) ? 0L : totalAttempted;
        totalCorrect = (totalCorrect == null) ? 0L : totalCorrect;
        totalStars = (totalStars == null) ? 0L : totalStars;

        double accuracy = (totalAttempted == 0) ? 0.0
                : Math.round((double) totalCorrect / totalAttempted * 1000.0) / 10.0;

        List<ProgressResponse.SessionSummary> recent = sessionRepo
                .findTop10ByOrderByCreatedAtDesc()
                .stream()
                .map(this::toSummary)
                .collect(Collectors.toList());

        ProgressResponse resp = new ProgressResponse();
        resp.setTotalQuestionsAttempted(totalAttempted);
        resp.setTotalCorrectAnswers(totalCorrect);
        resp.setAccuracyPercentage(accuracy);
        resp.setTotalStarsEarned(totalStars);
        resp.setRecentSessions(recent);
        return resp;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private boolean isAnswerCorrect(String userAnswer, double correctAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) return false;

        String cleaned = userAnswer.trim().toLowerCase()
                .replaceAll("[^0-9.\\-]", "");

        if (cleaned.isBlank()) {
            cleaned = wordToNumber(userAnswer.trim().toLowerCase());
        }

        try {
            double parsed = Double.parseDouble(cleaned);
            return Math.abs(parsed - correctAnswer) < 0.01;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String wordToNumber(String word) {
        return switch (word) {
            case "zero" -> "0";
            case "one" -> "1";
            case "two" -> "2";
            case "three" -> "3";
            case "four" -> "4";
            case "five" -> "5";
            case "six" -> "6";
            case "seven" -> "7";
            case "eight" -> "8";
            case "nine" -> "9";
            case "ten" -> "10";
            case "eleven" -> "11";
            case "twelve" -> "12";
            case "thirteen" -> "13";
            case "fourteen" -> "14";
            case "fifteen" -> "15";
            case "sixteen" -> "16";
            case "seventeen" -> "17";
            case "eighteen" -> "18";
            case "nineteen" -> "19";
            case "twenty" -> "20";
            case "thirty" -> "30";
            case "forty" -> "40";
            case "fifty" -> "50";
            default -> word;
        };
    }

    private double toDouble(Object val) {
        if (val instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private String sanitize(String val, String defaultVal) {
        return (val == null || val.isBlank()) ? defaultVal : val.trim().toLowerCase();
    }

    private ProgressResponse.SessionSummary toSummary(QuizSession s) {
        ProgressResponse.SessionSummary sum = new ProgressResponse.SessionSummary();
        sum.setId(s.getId());
        sum.setTopic(s.getTopic());
        sum.setDifficulty(s.getDifficulty());
        sum.setQuestionsAttempted(s.getQuestionsAttempted());
        sum.setCorrectAnswers(s.getCorrectAnswers());
        sum.setScore(s.getScore());
        sum.setStarsEarned(s.getStarsEarned());
        sum.setCreatedAt(s.getCreatedAt() != null ? s.getCreatedAt().format(FORMATTER) : "");
        return sum;
    }
}
