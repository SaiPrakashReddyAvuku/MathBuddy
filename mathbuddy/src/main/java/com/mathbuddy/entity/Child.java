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
 * Represents a child learner profile in MathBuddy.
 */
@Entity
@Table(name = "children")
@Getter
@Setter
@NoArgsConstructor
public class Child {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Integer age;

    @Column(length = 50)
    private String avatar; // e.g. "owl", "fox", "frog", "bunny", "lion", "bear"

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<QuizSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "child", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<QuestionAttempt> attempts = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (avatar == null || avatar.isBlank()) {
            avatar = "owl";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Child(String name, Integer age, String avatar) {
        this.name = name;
        this.age = age;
        this.avatar = (avatar != null && !avatar.isBlank()) ? avatar : "owl";
    }
}

