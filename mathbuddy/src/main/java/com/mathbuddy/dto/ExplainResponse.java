package com.mathbuddy.dto;

/**
 * Returned by /api/quiz/explain.
 */
public class ExplainResponse {

    private String explanation;
    private MathExplanationDto details;
    private boolean success;
    private String errorMessage;

    public ExplainResponse() {}

    public static ExplainResponse of(String explanation) {
        ExplainResponse r = new ExplainResponse();
        r.explanation = explanation;
        r.success     = true;
        return r;
    }

    public static ExplainResponse of(String explanation, MathExplanationDto details) {
        ExplainResponse r = of(explanation);
        r.details     = details;
        return r;
    }

    public static ExplainResponse error(String message) {
        ExplainResponse r = new ExplainResponse();
        r.success      = false;
        r.errorMessage = message;
        return r;
    }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public MathExplanationDto getDetails() { return details; }
    public void setDetails(MathExplanationDto details) { this.details = details; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

