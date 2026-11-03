package com.mathbuddy.service;

import com.mathbuddy.dto.ProgressResponse;
import com.mathbuddy.repository.QuizSessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles progress/dashboard logic (separated from quiz flow for clarity).
 */
@Service
public class ProgressService {

    private final QuizSessionRepository sessionRepo;
    private final QuizService quizService;

    public ProgressService(QuizSessionRepository sessionRepo, QuizService quizService) {
        this.sessionRepo = sessionRepo;
        this.quizService = quizService;
    }

    public ProgressResponse getProgress() {
        return quizService.getProgress();
    }

    public List<ProgressResponse.SessionSummary> getRecentSessions() {
        return quizService.getProgress().getRecentSessions();
    }
}

