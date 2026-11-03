package com.mathbuddy.controller;

import com.mathbuddy.dto.VoiceUnderstandRequest;
import com.mathbuddy.dto.VoiceUnderstandResponse;
import com.mathbuddy.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for LLM Voice Intent Understanding.
 * Accepts the recognized speech from the frontend, uses Gemini to understand
 * the child's natural language request, executes the backend action,
 * persists the results in MySQL, and returns structured JSON with the spoken response.
 */
@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private final QuizService quizService;

    public VoiceController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * POST /api/voice/understand
     * Accepts: { "speechText": "Give me an easy addition question", ... }
     * Returns structured JSON with detected intent and executed action result.
     */
    @PostMapping("/understand")
    public ResponseEntity<VoiceUnderstandResponse> understandVoice(@RequestBody VoiceUnderstandRequest request) {
        try {
            VoiceUnderstandResponse response = quizService.processVoiceInput(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(VoiceUnderstandResponse.error("Error understanding voice: " + e.getMessage()));
        }
    }
}

