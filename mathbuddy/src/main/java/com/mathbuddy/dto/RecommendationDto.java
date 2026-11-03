package com.mathbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Child-friendly personalized practice recommendation grounded in real learning data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDto {

    private String topic; // e.g. "addition", "subtraction"
    private String difficulty; // "easy", "medium", "hard"
    private String title; // "Addition Explorer 🚀"
    private String message; // "You are doing great! Let's try 3 more addition games to earn another star!"
    private String reason; // "Based on your recent practice"
    private String icon; // "➕", "🦉", "⭐"
    private String actionType; // "PRACTICE_TOPIC", "LEVEL_UP", "EXPLORE_NEW"
}

