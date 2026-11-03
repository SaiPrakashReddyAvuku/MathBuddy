package com.mathbuddy.dto;

import java.util.List;

/**
 * Returned by /api/progress and /api/progress/recent.
 */
public class ProgressResponse {

    private long totalQuestionsAttempted;
    private long totalCorrectAnswers;
    private double accuracyPercentage;
    private long totalStarsEarned;
    private List<SessionSummary> recentSessions;

    public ProgressResponse() {}

    // ── Inner class for individual session summary ────────────────────────────

    public static class SessionSummary {
        private Long id;
        private String topic;
        private String difficulty;
        private int questionsAttempted;
        private int correctAnswers;
        private int score;
        private int starsEarned;
        private String createdAt;

        public SessionSummary() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }

        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

        public int getQuestionsAttempted() { return questionsAttempted; }
        public void setQuestionsAttempted(int questionsAttempted) { this.questionsAttempted = questionsAttempted; }

        public int getCorrectAnswers() { return correctAnswers; }
        public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }

        public int getScore() { return score; }
        public void setScore(int score) { this.score = score; }

        public int getStarsEarned() { return starsEarned; }
        public void setStarsEarned(int starsEarned) { this.starsEarned = starsEarned; }

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getTotalQuestionsAttempted() { return totalQuestionsAttempted; }
    public void setTotalQuestionsAttempted(long totalQuestionsAttempted) { this.totalQuestionsAttempted = totalQuestionsAttempted; }

    public long getTotalCorrectAnswers() { return totalCorrectAnswers; }
    public void setTotalCorrectAnswers(long totalCorrectAnswers) { this.totalCorrectAnswers = totalCorrectAnswers; }

    public double getAccuracyPercentage() { return accuracyPercentage; }
    public void setAccuracyPercentage(double accuracyPercentage) { this.accuracyPercentage = accuracyPercentage; }

    public long getTotalStarsEarned() { return totalStarsEarned; }
    public void setTotalStarsEarned(long totalStarsEarned) { this.totalStarsEarned = totalStarsEarned; }

    public List<SessionSummary> getRecentSessions() { return recentSessions; }
    public void setRecentSessions(List<SessionSummary> recentSessions) { this.recentSessions = recentSessions; }
}

