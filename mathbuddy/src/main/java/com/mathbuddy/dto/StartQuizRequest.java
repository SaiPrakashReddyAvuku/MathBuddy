package com.mathbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Sent by the frontend when starting a quiz session.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StartQuizRequest {

    private Long childId; // Active child profile ID
    private String topic; // addition | subtraction | multiplication | division
    private String difficulty; // easy | medium | hard

    public StartQuizRequest(String topic, String difficulty) {
        this.topic = topic;
        this.difficulty = difficulty;
    }
}
