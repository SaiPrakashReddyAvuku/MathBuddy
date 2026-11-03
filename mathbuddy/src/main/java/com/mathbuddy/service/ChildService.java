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
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChildService {

    private static final Logger log = LoggerFactory.getLogger(ChildService.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    private final ChildRepository childRepo;
    private final QuizSessionRepository sessionRepo;
    private final QuestionAttemptRepository attemptRepo;

    // Cache to prevent duplicate child creation caused by rapid button double-clicks
    private final java.util.concurrent.ConcurrentHashMap<String, ChildResponse> recentCreations = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<String, Long> creationTimestamps = new java.util.concurrent.ConcurrentHashMap<>();

    public ChildService(ChildRepository childRepo,
                        QuizSessionRepository sessionRepo,
                        QuestionAttemptRepository attemptRepo) {
        this.childRepo = childRepo;
        this.sessionRepo = sessionRepo;
        this.attemptRepo = attemptRepo;
    }

    // ── CRUD Operations ───────────────────────────────────────────────────────

    @Transactional
    public ChildResponse createChild(ChildRequest req) {
        String cleanName = req.getName() != null ? req.getName().trim() : "";
        if (cleanName.isBlank()) {
            throw new IllegalArgumentException("Child name cannot be empty");
        }
        if (req.getAge() == null || req.getAge() < 3 || req.getAge() > 14) {
            throw new IllegalArgumentException("Child age must be between 3 and 14");
        }

        String avatar = (req.getAvatar() != null && !req.getAvatar().isBlank()) ? req.getAvatar() : "owl";

        // Duplicate prevention key (rapid multi-clicks within 2 seconds)
        String dedupeKey = cleanName.toLowerCase() + ":" + req.getAge() + ":" + avatar;
        long now = System.currentTimeMillis();
        Long lastTime = creationTimestamps.get(dedupeKey);
        if (lastTime != null && (now - lastTime) < 2000) {
            ChildResponse cached = recentCreations.get(dedupeKey);
            if (cached != null) {
                log.info("Prevented duplicate child creation for key: {}", dedupeKey);
                return cached;
            }
        }

        Child child = new Child(cleanName, req.getAge(), avatar);
        child = childRepo.save(child);
        log.info("Created child profile: id={} name={}", child.getId(), child.getName());

        ChildResponse response = toChildResponse(child);
        creationTimestamps.put(dedupeKey, now);
        recentCreations.put(dedupeKey, response);
        return response;
    }

    @Transactional(readOnly = true)
    public List<ChildResponse> getAllChildren() {
        return childRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toChildResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ChildResponse getChildById(Long id) {
        Child child = childRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Child profile not found: " + id));
        return toChildResponse(child);
    }

    @Transactional
    public ChildResponse updateChild(Long id, ChildRequest req) {
        Child child = childRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Child profile not found: " + id));

        child.setName(req.getName().trim());
        child.setAge(req.getAge());
        if (req.getAvatar() != null && !req.getAvatar().isBlank()) {
            child.setAvatar(req.getAvatar());
        }
        child = childRepo.save(child);
        log.info("Updated child profile: id={}", id);
        return toChildResponse(child);
    }

    @Transactional
    public void deleteChild(Long id) {
        if (!childRepo.existsById(id)) {
            throw new IllegalArgumentException("Child profile not found: " + id);
        }
        childRepo.deleteById(id);
        log.info("Deleted child profile: id={}", id);
    }

    // ── Progress & Recommendations ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ChildProgressResponse getChildProgress(Long childId) {
        Child child = childRepo.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child profile not found: " + childId));

        Long totalQuizzes = sessionRepo.countSessionsByChildId(childId);
        Long totalAttempted = sessionRepo.sumQuestionsAttemptedByChildId(childId);
        Long totalCorrect = sessionRepo.sumCorrectAnswersByChildId(childId);
        Long totalStars = sessionRepo.sumStarsEarnedByChildId(childId);

        totalQuizzes = (totalQuizzes == null) ? 0L : totalQuizzes;
        totalAttempted = (totalAttempted == null) ? 0L : totalAttempted;
        totalCorrect = (totalCorrect == null) ? 0L : totalCorrect;
        totalStars = (totalStars == null) ? 0L : totalStars;

        double overallAccuracy = (totalAttempted == 0) ? 0.0
                : Math.round((double) totalCorrect / totalAttempted * 1000.0) / 10.0;

        // Topic breakdown
        List<TopicProgressDto> topicStats = calculateTopicProgress(childId);

        // Recommendations based on real data
        List<RecommendationDto> recommendations = generatePersonalizedRecommendations(child, topicStats, childId);

        // Recent sessions
        List<ProgressResponse.SessionSummary> recentSessions = sessionRepo
                .findTop10ByChildIdOrderByCreatedAtDesc(childId)
                .stream()
                .map(this::toSessionSummary)
                .collect(Collectors.toList());

        return ChildProgressResponse.builder()
                .childId(child.getId())
                .childName(child.getName())
                .childAge(child.getAge())
                .childAvatar(child.getAvatar())
                .totalQuizzes(totalQuizzes)
                .totalQuestions(totalAttempted)
                .totalCorrect(totalCorrect)
                .totalStars(totalStars)
                .overallAccuracy(overallAccuracy)
                .topics(topicStats)
                .recommendations(recommendations)
                .recentSessions(recentSessions)
                .build();
    }

    @Transactional(readOnly = true)
    public List<RecommendationDto> getRecommendations(Long childId) {
        Child child = childRepo.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("Child profile not found: " + childId));
        List<TopicProgressDto> topicStats = calculateTopicProgress(childId);
        return generatePersonalizedRecommendations(child, topicStats, childId);
    }

    // ── Helper: Calculate Topic-Level Progress ─────────────────────────────────

    private List<TopicProgressDto> calculateTopicProgress(Long childId) {
        String[] supportedTopics = {"addition", "subtraction", "multiplication", "division"};
        Map<String, String> icons = Map.of(
                "addition", "➕",
                "subtraction", "➖",
                "multiplication", "✖️",
                "division", "➗"
        );

        List<Object[]> rawStats = attemptRepo.findTopicStatsByChildId(childId);
        Map<String, long[]> statsMap = new HashMap<>();
        for (Object[] row : rawStats) {
            String topic = ((String) row[0]).toLowerCase();
            long count = ((Number) row[1]).longValue();
            long correct = ((Number) row[2]).longValue();
            statsMap.put(topic, new long[]{count, correct});
        }

        List<TopicProgressDto> result = new ArrayList<>();
        for (String topic : supportedTopics) {
            long[] counts = statsMap.getOrDefault(topic, new long[]{0, 0});
            long attempted = counts[0];
            long correct = counts[1];

            double accuracy = 0.0;
            String status;
            String friendlyLabel;
            String statusColor;

            if (attempted == 0) {
                status = "Not practiced yet";
                friendlyLabel = "Ready to Try! 🎈";
                statusColor = "#FFF8E8";
            } else {
                accuracy = Math.round((double) correct / attempted * 1000.0) / 10.0;
                if (accuracy >= 80.0 && attempted >= 2) {
                    status = "Strong";
                    friendlyLabel = "Superstar! 🌟";
                    statusColor = "#C9F2D0";
                } else if (accuracy >= 50.0) {
                    status = "Improving";
                    friendlyLabel = "Getting Better! 🚀";
                    statusColor = "#BFE8FF";
                } else {
                    status = "Needs practice";
                    friendlyLabel = "Let's Practice! 💪";
                    statusColor = "#FFD6E7";
                }
            }

            result.add(TopicProgressDto.builder()
                    .topic(topic)
                    .icon(icons.getOrDefault(topic, "🔢"))
                    .totalAttempted(attempted)
                    .correctAnswers(correct)
                    .accuracyPercentage(accuracy)
                    .status(status)
                    .friendlyLabel(friendlyLabel)
                    .statusColor(statusColor)
                    .build());
        }

        return result;
    }

    // ── Helper: Personalized Recommendations (Grounded in Real Data) ───────────

    private List<RecommendationDto> generatePersonalizedRecommendations(
            Child child, List<TopicProgressDto> topics, Long childId) {
        List<RecommendationDto> recommendations = new ArrayList<>();

        // 1. Check for recent incorrect questions
        List<QuestionAttempt> recentIncorrect = attemptRepo.findRecentIncorrectByChildId(childId);
        if (!recentIncorrect.isEmpty()) {
            QuestionAttempt lastWrong = recentIncorrect.get(0);
            String wrongTopic = capitalize(lastWrong.getTopic());
            recommendations.add(RecommendationDto.builder()
                    .topic(lastWrong.getTopic())
                    .difficulty("easy")
                    .title("Practice " + wrongTopic + " 🎯")
                    .message("You recently tried a " + wrongTopic.toLowerCase() + " problem. Let's do 3 quick number games together to master it!")
                    .reason("Practice makes perfect!")
                    .icon(getTopicIcon(lastWrong.getTopic()))
                    .actionType("PRACTICE_TOPIC")
                    .build());
        }

        // 2. Check for "Needs practice" topics
        for (TopicProgressDto t : topics) {
            if ("Needs practice".equalsIgnoreCase(t.getStatus())) {
                String capTopic = capitalize(t.getTopic());
                recommendations.add(RecommendationDto.builder()
                        .topic(t.getTopic())
                        .difficulty("easy")
                        .title("Boost Your " + capTopic + " 🚀")
                        .message("Let's play with " + capTopic.toLowerCase() + "! Barnaby the Owl will help you step by step.")
                        .reason("Current accuracy is " + t.getAccuracyPercentage() + "%")
                        .icon(t.getIcon())
                        .actionType("PRACTICE_TOPIC")
                        .build());
                break; // One practice recommendation is enough
            }
        }

        // 3. Check for "Strong" topics to level up
        for (TopicProgressDto t : topics) {
            if ("Strong".equalsIgnoreCase(t.getStatus())) {
                String capTopic = capitalize(t.getTopic());
                recommendations.add(RecommendationDto.builder()
                        .topic(t.getTopic())
                        .difficulty("medium")
                        .title("Level Up: Medium " + capTopic + " ⭐")
                        .message("You're a superstar at " + capTopic.toLowerCase() + "! Are you ready to try bigger numbers on Medium difficulty?")
                        .reason("Scored " + t.getAccuracyPercentage() + "% accuracy!")
                        .icon("🏆")
                        .actionType("LEVEL_UP")
                        .build());
                break;
            }
        }

        // 4. Check for unpracticed topics
        for (TopicProgressDto t : topics) {
            if ("Not practiced yet".equalsIgnoreCase(t.getStatus())) {
                String capTopic = capitalize(t.getTopic());
                recommendations.add(RecommendationDto.builder()
                        .topic(t.getTopic())
                        .difficulty("easy")
                        .title("Discover " + capTopic + " 🎈")
                        .message("You haven't tried " + capTopic.toLowerCase() + " games yet! Tap here to explore fun puzzles together!")
                        .reason("New adventure awaiting!")
                        .icon(t.getIcon())
                        .actionType("EXPLORE_NEW")
                        .build());
                break;
            }
        }

        // Fallback friendly recommendation if all are answered
        if (recommendations.isEmpty()) {
            recommendations.add(RecommendationDto.builder()
                    .topic("addition")
                    .difficulty("easy")
                    .title("Daily Math Fun! 🌟")
                    .message("Ready to earn more stars today, " + child.getName() + "? Let's play an addition quiz!")
                    .reason("Keep your math streak glowing!")
                    .icon("🦉")
                    .actionType("PRACTICE_TOPIC")
                    .build());
        }

        return recommendations;
    }

    // ── Mapping Helpers ───────────────────────────────────────────────────────

    private ChildResponse toChildResponse(Child child) {
        Long stars = sessionRepo.sumStarsEarnedByChildId(child.getId());
        Long quizzes = sessionRepo.countSessionsByChildId(child.getId());
        Long attempted = sessionRepo.sumQuestionsAttemptedByChildId(child.getId());
        Long correct = sessionRepo.sumCorrectAnswersByChildId(child.getId());

        stars = (stars == null) ? 0L : stars;
        quizzes = (quizzes == null) ? 0L : quizzes;
        attempted = (attempted == null) ? 0L : attempted;
        correct = (correct == null) ? 0L : correct;

        double accuracy = (attempted == 0) ? 0.0
                : Math.round((double) correct / attempted * 1000.0) / 10.0;

        return ChildResponse.builder()
                .id(child.getId())
                .name(child.getName())
                .age(child.getAge())
                .avatar(child.getAvatar())
                .createdAt(child.getCreatedAt() != null ? child.getCreatedAt().format(FORMATTER) : "")
                .totalStars(stars)
                .totalQuizzes(quizzes)
                .overallAccuracy(accuracy)
                .build();
    }

    private ProgressResponse.SessionSummary toSessionSummary(QuizSession s) {
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

    private String capitalize(String str) {
        if (str == null || str.isBlank()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String getTopicIcon(String topic) {
        if (topic == null) return "🔢";
        return switch (topic.toLowerCase()) {
            case "addition" -> "➕";
            case "subtraction" -> "➖";
            case "multiplication" -> "✖️";
            case "division" -> "➗";
            default -> "🔢";
        };
    }
}

