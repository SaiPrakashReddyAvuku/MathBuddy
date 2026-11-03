package com.mathbuddy.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tracks individual question attempts for fine-grained progress analysis
 * and personalized practice recommendations.
 */
@Entity
@Table(name = "question_attempts")
@Getter
@Setter
@NoArgsConstructor
public class QuestionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession quizSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(name = "question_text", nullable = false, length = 500)
    private String questionText;

    @Column(nullable = false, length = 50)
    private String topic; // addition, subtraction, multiplication, division

    @Column(name = "expected_answer", nullable = false)
    private double expectedAnswer;

    @Column(name = "child_answer", length = 100)
    private String childAnswer;

    @Column(nullable = false)
    private boolean correct;

    @Column(length = 1000)
    private String explanation;

    @Column(name = "attempt_timestamp")
    private LocalDateTime attemptTimestamp;

    @PrePersist
    protected void onCreate() {
        attemptTimestamp = LocalDateTime.now();
    }

    public QuestionAttempt(QuizSession quizSession, Child child, String questionText,
                           String topic, double expectedAnswer, String childAnswer,
                           boolean correct, String explanation) {
        this.quizSession = quizSession;
        this.child = child;
        this.questionText = questionText;
        this.topic = topic;
        this.expectedAnswer = expectedAnswer;
        this.childAnswer = childAnswer;
        this.correct = correct;
        this.explanation = explanation;
        this.attemptTimestamp = LocalDateTime.now();
    }
}

