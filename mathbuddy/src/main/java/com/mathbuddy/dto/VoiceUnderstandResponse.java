package com.mathbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured response for POST /api/voice/understand
 * Contains the detected intent, extracted parameters, and the result of the backend action.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceUnderstandResponse {

    /**
     * Detected intent:
     * START_QUIZ, ANSWER_QUESTION, EXPLAIN, REPEAT, NEXT_QUESTION, CHANGE_DIFFICULTY, CHANGE_TOPIC, UNKNOWN
     */
    private String intent;

    /** Extracted topic (e.g., addition, subtraction, multiplication, division) */
    private String topic;

    /** Extracted difficulty (e.g., easy, medium, hard) */
    private String difficulty;

    /** Extracted numeric or textual answer if intent is ANSWER_QUESTION */
    private String answer;

    /** Spoken feedback string intended to be read aloud by Web Speech TTS */
    private String spokenResponse;

    /** Backend action executed */
    private String action;

    /** Data if a quiz session was started or next question generated */
    private QuizResponse quizData;

    /** Data if an answer was validated */
    private AnswerResponse answerData;

    /** Data if an explanation was requested */
    private ExplainResponse explainData;

    private boolean success;
    private String errorMessage;

    public static VoiceUnderstandResponse error(String message) {
        return VoiceUnderstandResponse.builder()
                .success(false)
                .errorMessage(message)
                .spokenResponse("Sorry, I had a little trouble understanding that. Could you say it again?")
                .build();
    }
}

