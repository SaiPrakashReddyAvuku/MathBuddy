package com.mathbuddy.repository;

import com.mathbuddy.entity.QuestionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionAttemptRepository extends JpaRepository<QuestionAttempt, Long> {

    List<QuestionAttempt> findByChildIdOrderByAttemptTimestampDesc(Long childId);

    List<QuestionAttempt> findByQuizSessionIdOrderByAttemptTimestampAsc(Long sessionId);

    @Query("SELECT COUNT(q) FROM QuestionAttempt q WHERE q.child.id = :childId")
    long countByChildId(@Param("childId") Long childId);

    @Query("SELECT COUNT(q) FROM QuestionAttempt q WHERE q.child.id = :childId AND q.correct = true")
    long countCorrectByChildId(@Param("childId") Long childId);

    @Query("SELECT q.topic, COUNT(q), SUM(CASE WHEN q.correct = true THEN 1 ELSE 0 END) " +
           "FROM QuestionAttempt q WHERE q.child.id = :childId GROUP BY q.topic")
    List<Object[]> findTopicStatsByChildId(@Param("childId") Long childId);

    @Query("SELECT q FROM QuestionAttempt q WHERE q.child.id = :childId AND q.correct = false ORDER BY q.attemptTimestamp DESC")
    List<QuestionAttempt> findRecentIncorrectByChildId(@Param("childId") Long childId);
}

