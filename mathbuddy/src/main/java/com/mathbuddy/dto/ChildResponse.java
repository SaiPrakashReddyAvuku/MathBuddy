package com.mathbuddy.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChildResponse {

    private Long id;
    private String name;
    private Integer age;
    private String avatar;
    private String createdAt;
    private long totalStars;
    private long totalQuizzes;
    private double overallAccuracy;
}

