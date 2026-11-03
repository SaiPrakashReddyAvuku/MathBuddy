package com.mathbuddy.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathbuddy.dto.MathExplanationDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Handles all communication with the Google Gemini API.
 *
 * Capabilities:
 * - Understands natural-language voice commands and extracts intent + parameters.
 * - Generates age-appropriate math questions for kids aged 6-12.
 * - Provides encouraging explanations and celebrations.
 * - 100% resilient: if the API key is missing, network fails, or timeout occurs,
 *   clean deterministic fallbacks ensure the demo never breaks.
 */
@Service
public class GeminiAiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-1.5-flash}")
    private String model;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MathExplanationBuilder explanationBuilder;

    public GeminiAiService(RestTemplate restTemplate, MathExplanationBuilder explanationBuilder) {
        this.restTemplate = restTemplate;
        this.explanationBuilder = explanationBuilder;
    }

    // ── Intent Understanding (Mandatory LLM Pipeline) ──────────────────────────

    /**
     * Ask Gemini to understand natural-language voice speech from the child.
     * Returns a Map with keys: intent, topic, difficulty, answer, spokenReply
     */
    public Map<String, Object> understandVoiceIntent(String speechText, String currentTopic,
                                                     String currentDifficulty, String currentQuestion) {
        if (speechText == null || speechText.trim().isEmpty()) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("intent", "UNKNOWN");
            fallback.put("spokenReply", "I didn't hear anything. Try clicking the mic and speaking again!");
            return fallback;
        }

        String prompt = buildIntentPrompt(speechText, currentTopic, currentDifficulty, currentQuestion);

        try {
            if (isKeyConfigured()) {
                String rawJson = callGemini(prompt);
                Map<String, Object> parsed = parseIntentJson(rawJson);
                if (parsed != null && !parsed.isEmpty()) {
                    return parsed;
                }
            } else {
                log.info("Gemini API key not provided. Using intelligent rule-based intent understanding.");
            }
        } catch (Exception e) {
            log.warn("Gemini intent understanding failed: {}. Using fallback intent analysis.", e.getMessage());
        }

        return fallbackIntentAnalysis(speechText, currentTopic, currentDifficulty);
    }

    // ── Question Generation ───────────────────────────────────────────────────

    /**
     * Ask Gemini to generate one math question.
     * @return a map with keys: question, answer, explanation, topic, difficulty
     */
    public Map<String, Object> generateQuestion(String topic, String difficulty) {
        String safeTopic = sanitizeTopic(topic);
        String safeDifficulty = sanitizeDifficulty(difficulty);

        String prompt = buildQuestionPrompt(safeTopic, safeDifficulty);
        try {
            if (isKeyConfigured()) {
                String rawJson = callGemini(prompt);
                return parseQuestionJson(rawJson, safeTopic, safeDifficulty);
            }
        } catch (Exception e) {
            log.warn("Gemini question generation failed: {}. Using fallback.", e.getMessage());
        }

        return fallbackQuestion(safeTopic, safeDifficulty);
    }

    // ── Explanations & Feedback ───────────────────────────────────────────────

    /**
     * Ask Gemini for a multi-step child-friendly explanation for "Owl's Math Adventure",
     * strictly grounded in the backend's verified correctAnswer.
     */
    public MathExplanationDto generateStructuredExplanation(String question, String userAnswer,
                                                            double correctAnswer, String topic) {
        if (isKeyConfigured()) {
            try {
                String prompt = buildStructuredExplanationPrompt(question, userAnswer, correctAnswer, topic);
                String rawJson = callGemini(prompt);
                MathExplanationDto dto = parseStructuredExplanationJson(rawJson, question, userAnswer, correctAnswer, topic);
                if (dto != null && !dto.getSteps().isEmpty()) {
                    dto.setCorrectAnswer(correctAnswer);
                    return dto;
                }
            } catch (Exception e) {
                log.warn("Gemini structured explanation failed: {}. Using deterministic builder.", e.getMessage());
            }
        }
        return explanationBuilder.buildExplanation(question, userAnswer, correctAnswer, topic);
    }

    /**
     * Returns a spoken narrative explanation summarizing Owl's Math Adventure.
     */
    public String generateExplanation(String question, String userAnswer,
                                      double correctAnswer, String topic) {
        MathExplanationDto exp = generateStructuredExplanation(question, userAnswer, correctAnswer, topic);
        if (exp != null) {
            String title = exp.getTitle() != null ? exp.getTitle() : "Let's learn together!";
            String finalExp = exp.getFinalExplanation() != null ? exp.getFinalExplanation() : "";
            return title + " " + finalExp + " You're doing great, let's try again!";
        }
        return "Let's look at the question together! The correct answer is " + formatNumber(correctAnswer) + ". You're learning every day! ⭐";
    }

    /**
     * Ask Gemini for positive feedback when the child answers correctly.
     */
    public String generatePositiveFeedback(String question, double answer) {
        String prompt = buildFeedbackPrompt(question, answer);
        try {
            if (isKeyConfigured()) {
                String rawJson = callGemini(prompt);
                return parseSimpleTextJson(rawJson, "feedback");
            }
        } catch (Exception e) {
            log.warn("Gemini feedback failed: {}. Using fallback.", e.getMessage());
        }

        return "Superstar! " + formatNumber(answer) + " is correct! You're doing amazing! ⭐";
    }

    // ── Private: Gemini API Call ──────────────────────────────────────────────

    private boolean isKeyConfigured() {
        return apiKey != null && !apiKey.trim().isEmpty() && !apiKey.contains("your-");
    }

    private String callGemini(String userPrompt) {
        String endpoint;
        if (apiUrl.contains(":generateContent")) {
            endpoint = apiUrl;
        } else {
            endpoint = String.format("%s/%s:generateContent", apiUrl, model);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-goog-api-key", apiKey.trim());

        Map<String, Object> textPart = Map.of("text", userPrompt);
        Map<String, Object> content = Map.of(
                "role", "user",
                "parts", List.of(textPart)
        );

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.6);
        generationConfig.put("maxOutputTokens", 500);
        generationConfig.put("responseMimeType", "application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(content));
        body.put("generationConfig", generationConfig);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Gemini API error: HTTP " + response.getStatusCode());
        }

        JsonNode root = parseJson(response.getBody());
        JsonNode candidates = root.path("candidates");
        if (candidates.isArray() && candidates.size() > 0) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray() && parts.size() > 0) {
                return parts.get(0).path("text").asText();
            }
        }

        throw new RuntimeException("Empty or invalid candidate response from Gemini API");
    }

    // ── Private: Prompt Builders ──────────────────────────────────────────────

    private String buildIntentPrompt(String speechText, String currentTopic,
                                     String currentDifficulty, String currentQuestion) {
        return """
            You are the AI brain of MathBuddy, an interactive voice math tutor for children aged 6-12.
            A child spoke the following sentence into the microphone:
            "%s"

            Current Context:
            - Current topic: %s
            - Current difficulty: %s
            - Currently displayed question: %s

            Identify the child's intent and parameters.
            Supported intents:
            - START_QUIZ (e.g. "start quiz", "give me an addition question", "let's practice math", "begin")
            - ANSWER_QUESTION (child gave an answer, e.g. "8", "the answer is twelve", "twenty five", "it is 4")
            - EXPLAIN (e.g. "explain this to me", "how do I solve this?", "why is that wrong?", "help me")
            - REPEAT (e.g. "repeat the question", "say that again", "what did you say?")
            - NEXT_QUESTION (e.g. "next question", "give me another one", "move on", "skip")
            - CHANGE_DIFFICULTY (e.g. "make it easier", "make it harder", "switch to medium", "change to hard")
            - CHANGE_TOPIC (e.g. "let's do multiplication", "change to division", "practice subtraction")
            - UNKNOWN (unrelated speech)

            Return ONLY valid JSON matching this schema:
            {
              "intent": "START_QUIZ",
              "topic": "addition",
              "difficulty": "easy",
              "answer": "8",
              "spokenReply": "Great! Starting an easy addition quiz!"
            }
            Ensure topic is one of: addition, subtraction, multiplication, division (or null).
            Ensure difficulty is one of: easy, medium, hard (or null).
            Ensure answer is normalized as digits/numbers if present (or null).
            Make spokenReply warm, friendly, and concise (1 short sentence) suitable for text-to-speech to a child.
            """.formatted(speechText, currentTopic, currentDifficulty, currentQuestion);
    }

    private String buildQuestionPrompt(String topic, String difficulty) {
        int[] range = getNumberRange(difficulty);
        return """
            You are a kind, friendly math tutor for children aged 6-12.
            Generate ONE age-appropriate math question for the topic "%s" at difficulty level "%s".
            
            Number range for this difficulty: %d to %d.
            Rules:
            - For division: ensure numbers divide evenly with no remainder.
            - For subtraction: ensure the answer is not negative.
            - The answer must be a single whole number.
            
            Respond ONLY with valid JSON in this exact structure:
            {
              "question": "What is 4 + 5?",
              "answer": 9,
              "explanation": "Start at 4 and count up 5 more to reach 9.",
              "topic": "%s",
              "difficulty": "%s"
            }
            """.formatted(topic, difficulty, range[0], range[1], topic, difficulty);
    }

    private String buildStructuredExplanationPrompt(String question, String userAnswer,
                                                    double correctAnswer, String topic) {
        String formattedAnswer = formatNumber(correctAnswer);
        return """
            You are Barnaby the friendly Owl math tutor for children aged 4-6.
            A child answered a math question incorrectly:
            Question: "%s"
            Child's answer: "%s"
            VERIFIED CORRECT ANSWER: %s (CRITICAL: You MUST use %s as the answer. Never change it!)
            Topic: %s

            Create a warm, simple, 3-step explanation suitable for a 5-year-old child.
            Use concrete child-friendly objects (apples, stars, cookies, or balloons).
            Never shame or criticize the child. Be enthusiastic and gentle!

            Respond ONLY with valid JSON in this exact structure:
            {
              "title": "Let's count together!",
              "operation": "ADDITION",
              "steps": [
                {
                  "stepNumber": 1,
                  "text": "We start with 4 sweet red apples.",
                  "visualType": "SHOW_GROUP",
                  "primaryCount": 4,
                  "secondaryCount": 0,
                  "totalCount": 4,
                  "highlightNote": "Starting apples"
                },
                {
                  "stepNumber": 2,
                  "text": "Our friend brings 2 more apples to share!",
                  "visualType": "ADD_GROUP",
                  "primaryCount": 4,
                  "secondaryCount": 2,
                  "totalCount": 6,
                  "highlightNote": "Adding apples"
                },
                {
                  "stepNumber": 3,
                  "text": "Let's count them all: 1, 2, 3, 4, 5, 6! 4 plus 2 equals 6!",
                  "visualType": "EQUATION",
                  "primaryCount": 4,
                  "secondaryCount": 2,
                  "totalCount": 6,
                  "highlightNote": "All together"
                }
              ],
              "finalExplanation": "4 plus 2 equals %s!",
              "followUpQuestion": "Can you count the apples with me?"
            }
            """.formatted(question, userAnswer, formattedAnswer, formattedAnswer, topic, formattedAnswer);
    }

    private MathExplanationDto parseStructuredExplanationJson(String content, String question, String userAnswer,
                                                              double correctAnswer, String topic) {
        try {
            String cleaned = cleanJsonFences(content);
            JsonNode root = parseJson(cleaned);

            MathExplanationDto dto = new MathExplanationDto();
            dto.setTitle(root.path("title").asText("Let's Solve This Together! 🌟"));
            dto.setQuestion(question);
            dto.setUserAnswer(userAnswer);
            dto.setCorrectAnswer(correctAnswer);
            dto.setOperation(root.path("operation").asText(topic.toUpperCase()));
            dto.setFinalExplanation(root.path("finalExplanation").asText("The correct answer is " + formatNumber(correctAnswer) + "."));
            dto.setFollowUpQuestion(root.path("followUpQuestion").asText("Can you say " + formatNumber(correctAnswer) + " with Barnaby?"));

            JsonNode stepsNode = root.path("steps");
            List<MathExplanationDto.Step> steps = new ArrayList<>();
            if (stepsNode.isArray()) {
                int idx = 1;
                for (JsonNode s : stepsNode) {
                    MathExplanationDto.Step step = new MathExplanationDto.Step(
                            s.path("stepNumber").asInt(idx++),
                            s.path("text").asText(""),
                            s.path("visualType").asText("SHOW_GROUP"),
                            s.path("primaryCount").asInt((int) correctAnswer),
                            s.path("secondaryCount").asInt(0),
                            s.path("totalCount").asInt((int) correctAnswer),
                            s.path("highlightNote").asText("")
                    );
                    steps.add(step);
                }
            }
            if (steps.isEmpty()) {
                return null;
            }
            dto.setSteps(steps);
            dto.setVisualMeta(new MathExplanationDto.VisualMeta("apple", (int) correctAnswer, 0, "="));
            return dto;
        } catch (Exception e) {
            log.warn("Could not parse structured explanation JSON: {}", e.getMessage());
            return null;
        }
    }

    private String buildExplanationPrompt(String question, String userAnswer,
                                          double correctAnswer, String topic) {
        return """
            You are an encouraging math tutor for young kids.
            A child got this question wrong:
            Question: %s
            Child's answer: %s
            Correct answer: %s
            Topic: %s

            Explain in 2 simple, friendly, encouraging sentences why the correct answer is %s.
            Include 1 fun emoji. Never make the child feel discouraged.

            Respond ONLY with valid JSON:
            {"explanation": "Your explanation here."}
            """.formatted(question, userAnswer, formatNumber(correctAnswer), topic, formatNumber(correctAnswer));
    }

    private String buildFeedbackPrompt(String question, double answer) {
        return """
            You are an energetic math tutor celebrating a child's correct answer!
            Question: %s
            Answer: %s

            Give a short, high-energy congratulatory cheer (1-2 sentences) with emojis (⭐, 🎉).
            Respond ONLY with valid JSON:
            {"feedback": "Your cheer here."}
            """.formatted(question, formatNumber(answer));
    }

    // ── Private: JSON Parsers ─────────────────────────────────────────────────

    private Map<String, Object> parseIntentJson(String content) {
        try {
            String cleaned = cleanJsonFences(content);
            JsonNode node = parseJson(cleaned);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("intent", node.path("intent").asText("UNKNOWN").toUpperCase());
            result.put("topic", node.hasNonNull("topic") ? sanitizeTopic(node.path("topic").asText()) : null);
            result.put("difficulty", node.hasNonNull("difficulty") ? sanitizeDifficulty(node.path("difficulty").asText()) : null);
            result.put("answer", node.hasNonNull("answer") ? node.path("answer").asText() : null);
            result.put("spokenReply", node.path("spokenReply").asText("I got that!"));
            return result;
        } catch (Exception e) {
            log.warn("Could not parse Gemini intent JSON: {}", content);
            return null;
        }
    }

    private Map<String, Object> parseQuestionJson(String content, String topic, String difficulty) {
        try {
            String cleaned = cleanJsonFences(content);
            JsonNode node = parseJson(cleaned);

            String question = node.path("question").asText();
            double answer = node.path("answer").asDouble();
            String explanation = node.path("explanation").asText();

            if (question.isBlank() || explanation.isBlank()) {
                throw new RuntimeException("Missing fields in Gemini question response");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", question);
            result.put("answer", answer);
            result.put("explanation", explanation);
            result.put("topic", topic);
            result.put("difficulty", difficulty);
            return result;
        } catch (Exception e) {
            log.warn("Could not parse Gemini question response: {}", content);
            return fallbackQuestion(topic, difficulty);
        }
    }

    private String parseSimpleTextJson(String content, String key) {
        try {
            String cleaned = cleanJsonFences(content);
            JsonNode node = parseJson(cleaned);
            String val = node.path(key).asText();
            if (!val.isBlank()) return val;
        } catch (Exception e) {
            log.warn("Could not parse Gemini text for key {}: {}", key, content);
        }
        return "You are doing great! Keep it up! ⭐";
    }

    private String cleanJsonFences(String raw) {
        if (raw == null) return "{}";
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceAll("```[a-zA-Z]*\\n?", "").replaceAll("```", "").trim();
        }
        return trimmed;
    }

    private JsonNode parseJson(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON: " + json, e);
        }
    }

    // ── Intelligent Fallback Intent Understanding ──────────────────────────────

    private Map<String, Object> fallbackIntentAnalysis(String speech, String curTopic, String curDiff) {
        String lower = speech.toLowerCase().trim();
        Map<String, Object> result = new LinkedHashMap<>();

        // Detect topic
        String detectedTopic = null;
        if (lower.contains("subtraction") || lower.contains("minus") || lower.contains("subtract")) detectedTopic = "subtraction";
        else if (lower.contains("multiplication") || lower.contains("times") || lower.contains("multiply")) detectedTopic = "multiplication";
        else if (lower.contains("division") || lower.contains("divide")) detectedTopic = "division";
        else if (lower.contains("addition") || lower.contains("plus") || lower.contains("add")) detectedTopic = "addition";

        // Detect difficulty
        String detectedDifficulty = null;
        if (lower.contains("easy") || lower.contains("easier") || lower.contains("simple")) detectedDifficulty = "easy";
        else if (lower.contains("hard") || lower.contains("harder") || lower.contains("difficult")) detectedDifficulty = "hard";
        else if (lower.contains("medium")) detectedDifficulty = "medium";

        // Check for START_QUIZ
        if (lower.contains("start") || lower.contains("begin") || lower.contains("play") || lower.contains("give me") || lower.contains("practice")) {
            result.put("intent", "START_QUIZ");
            result.put("topic", detectedTopic != null ? detectedTopic : (curTopic != null ? curTopic : "addition"));
            result.put("difficulty", detectedDifficulty != null ? detectedDifficulty : (curDiff != null ? curDiff : "easy"));
            result.put("spokenReply", "Awesome! Starting a quiz for you now!");
            return result;
        }

        // Check for EXPLAIN
        if (lower.contains("explain") || lower.contains("how") || lower.contains("why") || lower.contains("help")) {
            result.put("intent", "EXPLAIN");
            result.put("spokenReply", "Here is an explanation to help you understand!");
            return result;
        }

        // Check for REPEAT
        if (lower.contains("repeat") || lower.contains("again") || lower.contains("say that") || lower.contains("what was")) {
            result.put("intent", "REPEAT");
            result.put("spokenReply", "Sure! Let me repeat the question for you.");
            return result;
        }

        // Check for NEXT_QUESTION
        if (lower.contains("next") || lower.contains("skip") || lower.contains("another")) {
            result.put("intent", "NEXT_QUESTION");
            result.put("spokenReply", "Moving on to the next question!");
            return result;
        }

        // Check for CHANGE_DIFFICULTY
        if (detectedDifficulty != null && (lower.contains("make") || lower.contains("change") || lower.contains("switch") || lower.contains("set"))) {
            result.put("intent", "CHANGE_DIFFICULTY");
            result.put("difficulty", detectedDifficulty);
            result.put("spokenReply", "Setting difficulty to " + detectedDifficulty + "!");
            return result;
        }

        // Check for CHANGE_TOPIC
        if (detectedTopic != null && (lower.contains("topic") || lower.contains("change") || lower.contains("switch") || lower.contains("do "))) {
            result.put("intent", "CHANGE_TOPIC");
            result.put("topic", detectedTopic);
            result.put("spokenReply", "Switching topic to " + detectedTopic + "!");
            return result;
        }

        // Check for numerical answer (digits or word numbers)
        String extractedNum = extractSpokenNumber(lower);
        if (extractedNum != null) {
            result.put("intent", "ANSWER_QUESTION");
            result.put("answer", extractedNum);
            result.put("spokenReply", "Checking your answer: " + extractedNum);
            return result;
        }

        result.put("intent", "UNKNOWN");
        result.put("spokenReply", "I heard: \"" + speech + "\". Try saying your answer or 'Next question'!");
        return result;
    }

    private String extractSpokenNumber(String text) {
        // Direct digits extraction
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("-?\\b\\d+(\\.\\d+)?\\b").matcher(text);
        if (m.find()) {
            return m.group();
        }

        // English number words map
        Map<String, String> words = Map.ofEntries(
                Map.entry("zero", "0"), Map.entry("one", "1"), Map.entry("two", "2"),
                Map.entry("three", "3"), Map.entry("four", "4"), Map.entry("five", "5"),
                Map.entry("six", "6"), Map.entry("seven", "7"), Map.entry("eight", "8"),
                Map.entry("nine", "9"), Map.entry("ten", "10"), Map.entry("eleven", "11"),
                Map.entry("twelve", "12"), Map.entry("thirteen", "13"), Map.entry("fourteen", "14"),
                Map.entry("fifteen", "15"), Map.entry("sixteen", "16"), Map.entry("seventeen", "17"),
                Map.entry("eighteen", "18"), Map.entry("nineteen", "19"), Map.entry("twenty", "20"),
                Map.entry("thirty", "30"), Map.entry("forty", "40"), Map.entry("fifty", "50")
        );

        for (Map.Entry<String, String> entry : words.entrySet()) {
            if (text.matches(".*\\b" + entry.getKey() + "\\b.*")) {
                return entry.getValue();
            }
        }

        return null;
    }

    // ── Helper Utilities ──────────────────────────────────────────────────────

    private String sanitizeTopic(String topic) {
        if (topic == null) return "addition";
        String t = topic.toLowerCase().trim();
        return switch (t) {
            case "subtraction", "multiplication", "division" -> t;
            default -> "addition";
        };
    }

    private String sanitizeDifficulty(String difficulty) {
        if (difficulty == null) return "easy";
        String d = difficulty.toLowerCase().trim();
        return switch (d) {
            case "medium", "hard" -> d;
            default -> "easy";
        };
    }

    private int[] getNumberRange(String difficulty) {
        return switch (difficulty.toLowerCase()) {
            case "medium" -> new int[]{10, 50};
            case "hard"   -> new int[]{20, 100};
            default       -> new int[]{1, 10};
        };
    }

    private String formatNumber(double n) {
        if (n == Math.floor(n)) return String.valueOf((long) n);
        return String.valueOf(n);
    }

    /**
     * Deterministic fallback math question generator.
     */
    public Map<String, Object> fallbackQuestion(String topic, String difficulty) {
        Random rand = new Random();
        int[] range = getNumberRange(difficulty);
        int min = range[0], max = range[1];

        int a, b;
        String question;
        double answer;
        String explanation;

        switch (topic.toLowerCase()) {
            case "subtraction" -> {
                a = min + rand.nextInt(max - min + 1);
                b = rand.nextInt(a - min + 1) + min;
                question    = "What is " + a + " - " + b + "?";
                answer      = a - b;
                explanation = "Start at " + a + " and count back " + b + " steps to get " + (int)answer + ".";
            }
            case "multiplication" -> {
                a = min + rand.nextInt(Math.min(max, 12) - min + 1);
                b = 1 + rand.nextInt(10);
                question    = "What is " + a + " × " + b + "?";
                answer      = (double) a * b;
                explanation = "You can think of it as adding " + a + " a total of " + b + " times.";
            }
            case "division" -> {
                b = 2 + rand.nextInt(9);
                a = b * (min + rand.nextInt((max / b) - (min / b) + 1));
                question    = "What is " + a + " ÷ " + b + "?";
                answer      = (double) a / b;
                explanation = "Ask yourself: how many groups of " + b + " fit into " + a + "?";
            }
            default -> { // addition
                a = min + rand.nextInt(max - min + 1);
                b = min + rand.nextInt(max - min + 1);
                question    = "What is " + a + " + " + b + "?";
                answer      = a + b;
                explanation = "Start at " + a + " and count up " + b + " more to reach " + (int)answer + ".";
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question",    question);
        result.put("answer",      answer);
        result.put("explanation", explanation);
        result.put("topic",       topic);
        result.put("difficulty",  difficulty);
        return result;
    }
}

