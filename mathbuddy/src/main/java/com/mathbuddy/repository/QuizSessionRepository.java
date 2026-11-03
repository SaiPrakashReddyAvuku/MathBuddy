package com.mathbuddy.repository;

import com.mathbuddy.entity.QuizSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    // Returns the 10 most recent sessions (newest first)
    List<QuizSession> findTop10ByOrderByCreatedAtDesc();

    // Aggregate stats for the progress dashboard
    @Query("SELECT SUM(q.questionsAttempted) FROM QuizSession q")
    Long sumQuestionsAttempted();

    @Query("SELECT SUM(q.correctAnswers) FROM QuizSession q")
    Long sumCorrectAnswers();

    @Query("SELECT SUM(q.starsEarned) FROM QuizSession q")
    Long sumStarsEarned();

    // Child-specific queries
    List<QuizSession> findByChildIdOrderByCreatedAtDesc(Long childId);

    List<QuizSession> findTop10ByChildIdOrderByCreatedAtDesc(Long childId);

    @Query("SELECT SUM(q.questionsAttempted) FROM QuizSession q WHERE q.child.id = :childId")
    Long sumQuestionsAttemptedByChildId(@Param("childId") Long childId);

    @Query("SELECT SUM(q.correctAnswers) FROM QuizSession q WHERE q.child.id = :childId")
    Long sumCorrectAnswersByChildId(@Param("childId") Long childId);

    @Query("SELECT SUM(q.starsEarned) FROM QuizSession q WHERE q.child.id = :childId")
    Long sumStarsEarnedByChildId(@Param("childId") Long childId);

    @Query("SELECT COUNT(q) FROM QuizSession q WHERE q.child.id = :childId")
    Long countSessionsByChildId(@Param("childId") Long childId);
}
