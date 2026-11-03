package com.mathbuddy.dto;

/**
 * Returned by /api/quiz/start and /api/quiz/next.
 * Contains the new question plus the session id.
 */
public class QuizResponse {

    private Long sessionId;
    private String question;
    private double answer;          // numeric correct answer (used for validation)
    private String explanation;     // brief hint / context (shown after wrong answer)
    private String topic;
    private String difficulty;
    private boolean success;
    private String errorMessage;

    public QuizResponse() {}

    // ── Static factory methods ───────────────────────────────────────────────

    public static QuizResponse success(Long sessionId, String question, double answer,
                                       String explanation, String topic, String difficulty) {
        QuizResponse r = new QuizResponse();
        r.sessionId    = sessionId;
        r.question     = question;
        r.answer       = answer;
        r.explanation  = explanation;
        r.topic        = topic;
        r.difficulty   = difficulty;
        r.success      = true;
        return r;
    }

    public static QuizResponse error(String message) {
        QuizResponse r = new QuizResponse();
        r.success      = false;
        r.errorMessage = message;
        return r;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public double getAnswer() { return answer; }
    public void setAnswer(double answer) { this.answer = answer; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

