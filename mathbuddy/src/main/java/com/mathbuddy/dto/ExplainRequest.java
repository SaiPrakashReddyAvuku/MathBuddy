package com.mathbuddy.dto;

/**
 * Sent by the frontend when the child asks for an explanation or next question.
 */
public class ExplainRequest {

    private Long sessionId;
    private String question;
    private String topic;
    private String difficulty;
    private String userAnswer;
    private double correctAnswer;

    public ExplainRequest() {}

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public double getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(double correctAnswer) { this.correctAnswer = correctAnswer; }
}

