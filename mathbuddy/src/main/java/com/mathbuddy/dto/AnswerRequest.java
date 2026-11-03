package com.mathbuddy.dto;

/**
 * Sent by the frontend when the child submits an answer.
 */
public class AnswerRequest {

    private Long sessionId;        // which quiz session
    private String question;       // the question text shown to the child
    private String userAnswer;     // what the child said / typed
    private double correctAnswer;  // numeric correct answer (sent from backend, echoed back)

    public AnswerRequest() {}

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public double getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(double correctAnswer) { this.correctAnswer = correctAnswer; }
}

