package com.mathbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicProgressDto {

    private String topic;
    private String icon;
    private long totalAttempted;
    private long correctAnswers;
    private double accuracyPercentage;
    /**
     * "Strong" (>= 80%), "Improving" (50-79%), "Needs practice" (< 50%), "Not practiced yet" (0)
     */
    private String status;
    private String friendlyLabel; // "Superstar! 🌟", "Getting Better! 🚀", "Let's Practice! 💪", "Ready to Try! 🎈"
    private String statusColor; // token or hex
}

