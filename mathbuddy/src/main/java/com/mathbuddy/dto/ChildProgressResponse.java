package com.mathbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildProgressResponse {

    private Long childId;
    private String childName;
    private Integer childAge;
    private String childAvatar;
    private long totalQuizzes;
    private long totalQuestions;
    private long totalCorrect;
    private long totalStars;
    private double overallAccuracy;
    private List<TopicProgressDto> topics;
    private List<RecommendationDto> recommendations;
    private List<ProgressResponse.SessionSummary> recentSessions;
}

