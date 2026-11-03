package com.mathbuddy.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Structured DTO for "Owl's Math Adventure" multi-step child explanations.
 */
public class MathExplanationDto {

    private String title;
    private String question;
    private double correctAnswer;
    private String userAnswer;
    private String operation;       // ADDITION, SUBTRACTION, COUNTING, COMPARISON, MISSING_NUMBER, MULTIPLICATION, DIVISION, GENERAL
    private List<Step> steps = new ArrayList<>();
    private String finalExplanation;
    private String followUpQuestion;
    private VisualMeta visualMeta;

    public MathExplanationDto() {}

    public static class Step {
        private int stepNumber;
        private String text;
        private String visualType;  // SHOW_GROUP, ADD_GROUP, REMOVE_GROUP, COUNT_ITEMS, COMPARE_GROUPS, NUMBER_PATH, EQUATION
        private int primaryCount;
        private int secondaryCount;
        private int totalCount;
        private String highlightNote;

        public Step() {}

        public Step(int stepNumber, String text, String visualType, int primaryCount, int secondaryCount, int totalCount, String highlightNote) {
            this.stepNumber = stepNumber;
            this.text = text;
            this.visualType = visualType;
            this.primaryCount = primaryCount;
            this.secondaryCount = secondaryCount;
            this.totalCount = totalCount;
            this.highlightNote = highlightNote;
        }

        public int getStepNumber() { return stepNumber; }
        public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }

        public String getVisualType() { return visualType; }
        public void setVisualType(String visualType) { this.visualType = visualType; }

        public int getPrimaryCount() { return primaryCount; }
        public void setPrimaryCount(int primaryCount) { this.primaryCount = primaryCount; }

        public int getSecondaryCount() { return secondaryCount; }
        public void setSecondaryCount(int secondaryCount) { this.secondaryCount = secondaryCount; }

        public int getTotalCount() { return totalCount; }
        public void setTotalCount(int totalCount) { this.totalCount = totalCount; }

        public String getHighlightNote() { return highlightNote; }
        public void setHighlightNote(String highlightNote) { this.highlightNote = highlightNote; }
    }

    public static class VisualMeta {
        private String itemType;    // apple, star, cookie, balloon
        private int num1;
        private int num2;
        private String symbol;      // +, -, ×, ÷, vs, ?

        public VisualMeta() {}

        public VisualMeta(String itemType, int num1, int num2, String symbol) {
            this.itemType = itemType;
            this.num1 = num1;
            this.num2 = num2;
            this.symbol = symbol;
        }

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public int getNum1() { return num1; }
        public void setNum1(int num1) { this.num1 = num1; }

        public int getNum2() { return num2; }
        public void setNum2(int num2) { this.num2 = num2; }

        public String getSymbol() { return symbol; }
        public void setSymbol(String symbol) { this.symbol = symbol; }
    }

    // ── Getters and Setters ───────────────────────────────────────────────────

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public double getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(double correctAnswer) { this.correctAnswer = correctAnswer; }

    public String getUserAnswer() { return userAnswer; }
    public void setUserAnswer(String userAnswer) { this.userAnswer = userAnswer; }

    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }

    public List<Step> getSteps() { return steps; }
    public void setSteps(List<Step> steps) { this.steps = steps; }

    public String getFinalExplanation() { return finalExplanation; }
    public void setFinalExplanation(String finalExplanation) { this.finalExplanation = finalExplanation; }

    public String getFollowUpQuestion() { return followUpQuestion; }
    public void setFollowUpQuestion(String followUpQuestion) { this.followUpQuestion = followUpQuestion; }

    public VisualMeta getVisualMeta() { return visualMeta; }
    public void setVisualMeta(VisualMeta visualMeta) { this.visualMeta = visualMeta; }
}

