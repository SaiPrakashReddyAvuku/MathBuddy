package com.mathbuddy.controller;

import com.mathbuddy.dto.ProgressResponse;
import com.mathbuddy.service.ProgressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for the progress dashboard.
 */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    /**
     * GET /api/progress
     * Returns overall stats: total questions, accuracy, stars.
     */
    @GetMapping
    public ResponseEntity<ProgressResponse> getProgress() {
        try {
            return ResponseEntity.ok(progressService.getProgress());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/progress/recent
     * Returns the 10 most recent quiz sessions.
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ProgressResponse.SessionSummary>> getRecentSessions() {
        try {
            return ResponseEntity.ok(progressService.getRecentSessions());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}

