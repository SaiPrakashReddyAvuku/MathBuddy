package com.mathbuddy.dto;

/**
 * Returned by /api/quiz/answer.
 */
public class AnswerResponse {

    private boolean correct;
    private double correctAnswer;
    private String feedback;          // encouraging message to speak aloud
    private int score;
    private int questionsAttempted;
    private int correctAnswers;
    private int starsEarned;
    private MathExplanationDto explanation;
    private boolean success;
    private String errorMessage;

    public AnswerResponse() {}

    public static AnswerResponse of(boolean correct, double correctAnswer,
                                    String feedback, int score,
                                    int attempted, int correctCount, int stars) {
        AnswerResponse r = new AnswerResponse();
        r.correct            = correct;
        r.correctAnswer      = correctAnswer;
        r.feedback           = feedback;
        r.score              = score;
        r.questionsAttempted = attempted;
        r.correctAnswers     = correctCount;
        r.starsEarned        = stars;
        r.success            = true;
        return r;
    }

    public static AnswerResponse of(boolean correct, double correctAnswer,
                                    String feedback, int score,
                                    int attempted, int correctCount, int stars,
                                    MathExplanationDto explanation) {
        AnswerResponse r = of(correct, correctAnswer, feedback, score, attempted, correctCount, stars);
        r.explanation = explanation;
        return r;
    }

    public static AnswerResponse error(String message) {
        AnswerResponse r = new AnswerResponse();
        r.success      = false;
        r.errorMessage = message;
        return r;
    }

    // ── Getters & Setters ────────────────────────────────────────────────────

    public boolean isCorrect() { return correct; }
    public void setCorrect(boolean correct) { this.correct = correct; }

    public double getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(double correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public int getQuestionsAttempted() { return questionsAttempted; }
    public void setQuestionsAttempted(int questionsAttempted) { this.questionsAttempted = questionsAttempted; }

    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }

    public int getStarsEarned() { return starsEarned; }
    public void setStarsEarned(int starsEarned) { this.starsEarned = starsEarned; }

    public MathExplanationDto getExplanation() { return explanation; }
    public void setExplanation(MathExplanationDto explanation) { this.explanation = explanation; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

