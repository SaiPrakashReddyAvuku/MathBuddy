package com.mathbuddy.controller;

import com.mathbuddy.dto.*;
import com.mathbuddy.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for all quiz-related endpoints.
 */
@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    /**
     * POST /api/quiz/start
     * Starts a new quiz session and returns the first question.
     *
     * Request body:
     * { "topic": "addition", "difficulty": "easy" }
     */
    @PostMapping("/start")
    public ResponseEntity<QuizResponse> startQuiz(@RequestBody StartQuizRequest request) {
        try {
            QuizResponse response = quizService.startQuiz(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(QuizResponse.error("Could not start quiz: " + e.getMessage()));
        }
    }

    /**
     * POST /api/quiz/answer
     * Validates the child's answer and updates the score.
     *
     * Request body:
     * { "sessionId": 1, "question": "What is 3+5?", "userAnswer": "8", "correctAnswer": 8 }
     */
    @PostMapping("/answer")
    public ResponseEntity<AnswerResponse> submitAnswer(@RequestBody AnswerRequest request) {
        try {
            AnswerResponse response = quizService.checkAnswer(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(AnswerResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AnswerResponse.error("Could not validate answer: " + e.getMessage()));
        }
    }

    /**
     * POST /api/quiz/next
     * Generates the next question for an existing session.
     *
     * Request body:
     * { "sessionId": 1 }
     */
    @PostMapping("/next")
    public ResponseEntity<QuizResponse> nextQuestion(@RequestBody Map<String, Object> body) {
        try {
            Long sessionId = Long.valueOf(body.get("sessionId").toString());
            QuizResponse response = quizService.nextQuestion(sessionId, null, null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(QuizResponse.error("Could not get next question: " + e.getMessage()));
        }
    }

    /**
     * POST /api/quiz/explain
     * Generates a child-friendly explanation for the current question.
     *
     * Request body:
     * { "sessionId":1, "question":"...", "topic":"addition",
     *   "difficulty":"easy", "userAnswer":"3", "correctAnswer":8 }
     */
    @PostMapping("/explain")
    public ResponseEntity<ExplainResponse> explain(@RequestBody ExplainRequest request) {
        try {
            ExplainResponse response = quizService.explain(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ExplainResponse.error("Could not generate explanation: " + e.getMessage()));
        }
    }

    /**
     * POST /api/quiz/end
     * Marks the active session as COMPLETED.
     */
    @PostMapping("/end")
    public ResponseEntity<Void> endQuiz(@RequestBody Map<String, Object> body) {
        if (body.containsKey("sessionId")) {
            Long sessionId = Long.valueOf(body.get("sessionId").toString());
            quizService.endQuiz(sessionId);
        }
        return ResponseEntity.ok().build();
    }
}

