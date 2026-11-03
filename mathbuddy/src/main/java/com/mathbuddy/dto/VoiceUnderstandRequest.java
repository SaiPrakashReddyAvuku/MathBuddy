package com.mathbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for POST /api/voice/understand
 * Accepts recognized speech text and optional quiz context.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceUnderstandRequest {

    /** Active child profile ID */
    private Long childId;

    /** The text recognized by the Web Speech API */
    private String speechText;

    /** Current quiz session ID if active */
    private Long sessionId;

    /** Currently displayed question if any */
    private String currentQuestion;

    /** Correct answer of the current question if any */
    private Double currentAnswer;

    /** Currently selected topic */
    private String topic;

    /** Currently selected difficulty */
    private String difficulty;
}
