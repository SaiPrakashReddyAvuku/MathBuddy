package com.mathbuddy.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single quiz session, optionally associated with a Child profile.
 */
@Entity
@Table(name = "quiz_sessions")
@Getter
@Setter
@NoArgsConstructor
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "child_id")
    private Child child;

    @Column(nullable = false)
    private String topic; // addition, subtraction, multiplication, division

    @Column(nullable = false)
    private String difficulty; // easy, medium, hard

    @Column(name = "questions_attempted")
    private int questionsAttempted = 0;

    @Column(name = "correct_answers")
    private int correctAnswers = 0;

    @Column(name = "score")
    private int score = 0;

    @Column(name = "stars_earned")
    private int starsEarned = 0;

    @Column(length = 30)
    private String status = "IN_PROGRESS"; // IN_PROGRESS, COMPLETED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "quizSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<QuestionAttempt> questionAttempts = new ArrayList<>();

    // ── Lifecycle hooks ──────────────────────────────────────────────────────

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "IN_PROGRESS";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Constructors ─────────────────────────────────────────────────────────

    public QuizSession(String topic, String difficulty) {
        this.topic = topic;
        this.difficulty = difficulty;
        this.status = "IN_PROGRESS";
    }

    public QuizSession(Child child, String topic, String difficulty) {
        this.child = child;
        this.topic = topic;
        this.difficulty = difficulty;
        this.status = "IN_PROGRESS";
    }

    // ── Helper: calculate stars from accuracy ────────────────────────────────

    public void recalculateStars() {
        if (questionsAttempted == 0) {
            starsEarned = 0;
            return;
        }
        double accuracy = (double) correctAnswers / questionsAttempted * 100;
        if (accuracy >= 90) starsEarned = 3;
        else if (accuracy >= 60) starsEarned = 2;
        else if (accuracy >= 30) starsEarned = 1;
        else starsEarned = 0;
    }
}
