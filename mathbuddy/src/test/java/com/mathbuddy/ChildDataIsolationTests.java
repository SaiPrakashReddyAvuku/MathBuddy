package com.mathbuddy;

import com.mathbuddy.dto.*;
import com.mathbuddy.entity.Child;
import com.mathbuddy.entity.QuizSession;
import com.mathbuddy.repository.ChildRepository;
import com.mathbuddy.repository.QuestionAttemptRepository;
import com.mathbuddy.repository.QuizSessionRepository;
import com.mathbuddy.service.ChildService;
import com.mathbuddy.service.QuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that every child profile has a reliable unique identity (primary key),
 * that multiple children with the SAME name can exist independently,
 * and that quiz history, attempts, progress, and recommendations remain strictly isolated.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "gemini.api.key=test-key"
})
class ChildDataIsolationTests {

    @Autowired
    private ChildService childService;

    @Autowired
    private QuizService quizService;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private QuizSessionRepository sessionRepository;

    @Autowired
    private QuestionAttemptRepository attemptRepository;

    @BeforeEach
    void cleanUp() {
        attemptRepository.deleteAll();
        sessionRepository.deleteAll();
        childRepository.deleteAll();
    }

    @Test
    @DisplayName("Children with the exact same name must have unique IDs and separate data")
    void testTwoChildrenSameNameIndependentIdentity() {
        // 1. Create two children with the exact same name ("Leo")
        ChildRequest req1 = new ChildRequest("Leo", 5, "owl");
        ChildRequest req2 = new ChildRequest("Leo", 6, "fox");

        ChildResponse child1 = childService.createChild(req1);
        ChildResponse child2 = childService.createChild(req2);

        // Verify unique database IDs
        assertNotNull(child1.getId());
        assertNotNull(child2.getId());
        assertNotEquals(child1.getId(), child2.getId(), "Every child must have a unique database primary key");
        assertEquals("Leo", child1.getName());
        assertEquals("Leo", child2.getName());
        assertEquals(5, child1.getAge());
        assertEquals(6, child2.getAge());

        // 2. Start a quiz session for Child 1 (Addition)
        StartQuizRequest startReq1 = new StartQuizRequest();
        startReq1.setChildId(child1.getId());
        startReq1.setTopic("addition");
        startReq1.setDifficulty("easy");
        QuizResponse quiz1 = quizService.startQuiz(startReq1);
        assertNotNull(quiz1.getSessionId());

        // Child 1 answers 2 questions correctly
        AnswerRequest ans1 = new AnswerRequest();
        ans1.setSessionId(quiz1.getSessionId());
        ans1.setQuestion(quiz1.getQuestion());
        ans1.setUserAnswer(String.valueOf(quiz1.getAnswer()));
        ans1.setCorrectAnswer(quiz1.getAnswer());
        AnswerResponse ansResp1 = quizService.checkAnswer(ans1);
        assertTrue(ansResp1.isCorrect());

        QuizResponse q1Next = quizService.nextQuestion(quiz1.getSessionId(), "addition", "easy");
        AnswerRequest ans2 = new AnswerRequest();
        ans2.setSessionId(quiz1.getSessionId());
        ans2.setQuestion(q1Next.getQuestion());
        ans2.setUserAnswer(String.valueOf(q1Next.getAnswer()));
        ans2.setCorrectAnswer(q1Next.getAnswer());
        AnswerResponse ansResp2 = quizService.checkAnswer(ans2);
        assertTrue(ansResp2.isCorrect());

        quizService.endQuiz(quiz1.getSessionId());

        // 3. Start a quiz session for Child 2 (Subtraction)
        StartQuizRequest startReq2 = new StartQuizRequest();
        startReq2.setChildId(child2.getId());
        startReq2.setTopic("subtraction");
        startReq2.setDifficulty("easy");
        QuizResponse quiz2 = quizService.startQuiz(startReq2);
        assertNotNull(quiz2.getSessionId());
        assertNotEquals(quiz1.getSessionId(), quiz2.getSessionId());

        // Child 2 answers 1 wrong answer
        AnswerRequest ansWrong = new AnswerRequest();
        ansWrong.setSessionId(quiz2.getSessionId());
        ansWrong.setQuestion(quiz2.getQuestion());
        ansWrong.setUserAnswer("9999");
        ansWrong.setCorrectAnswer(quiz2.getAnswer());
        AnswerResponse ansWrongResp = quizService.checkAnswer(ansWrong);
        assertFalse(ansWrongResp.isCorrect());

        quizService.endQuiz(quiz2.getSessionId());

        // 4. Verify Child 1 Progress is completely isolated
        ChildProgressResponse prog1 = childService.getChildProgress(child1.getId());
        assertEquals("Leo", prog1.getChildName());
        assertEquals(child1.getId(), prog1.getChildId());
        assertEquals(1, prog1.getTotalQuizzes(), "Child 1 must have exactly 1 quiz session");
        assertEquals(2, prog1.getTotalQuestions(), "Child 1 must have exactly 2 question attempts");
        assertEquals(2, prog1.getTotalCorrect(), "Child 1 answered 2 correct");
        assertEquals(100.0, prog1.getOverallAccuracy());
        assertEquals(1, prog1.getRecentSessions().size());
        assertEquals(quiz1.getSessionId(), prog1.getRecentSessions().get(0).getId());

        // 5. Verify Child 2 Progress is completely isolated
        ChildProgressResponse prog2 = childService.getChildProgress(child2.getId());
        assertEquals("Leo", prog2.getChildName());
        assertEquals(child2.getId(), prog2.getChildId());
        assertEquals(1, prog2.getTotalQuizzes(), "Child 2 must have exactly 1 quiz session");
        assertEquals(1, prog2.getTotalQuestions(), "Child 2 must have exactly 1 question attempt");
        assertEquals(0, prog2.getTotalCorrect(), "Child 2 answered 0 correct");
        assertEquals(0.0, prog2.getOverallAccuracy());
        assertEquals(1, prog2.getRecentSessions().size());
        assertEquals(quiz2.getSessionId(), prog2.getRecentSessions().get(0).getId());

        // 6. Verify Topic Mastery isolation
        // Child 1: Addition must be "Strong"
        TopicProgressDto addProg1 = prog1.getTopics().stream()
                .filter(t -> "addition".equalsIgnoreCase(t.getTopic()))
                .findFirst().orElseThrow();
        assertEquals(2, addProg1.getTotalAttempted());
        assertEquals(2, addProg1.getCorrectAnswers());
        assertEquals("Strong", addProg1.getStatus());

        // Child 2: Addition must be "Not practiced yet", Subtraction must be "Needs practice"
        TopicProgressDto addProg2 = prog2.getTopics().stream()
                .filter(t -> "addition".equalsIgnoreCase(t.getTopic()))
                .findFirst().orElseThrow();
        assertEquals(0, addProg2.getTotalAttempted(), "Child 2 never practiced addition");
        assertEquals("Not practiced yet", addProg2.getStatus());

        TopicProgressDto subProg2 = prog2.getTopics().stream()
                .filter(t -> "subtraction".equalsIgnoreCase(t.getTopic()))
                .findFirst().orElseThrow();
        assertEquals(1, subProg2.getTotalAttempted());
        assertEquals(0, subProg2.getCorrectAnswers());
        assertEquals("Needs practice", subProg2.getStatus());

        // 7. Verify recommendations are unique to each child
        List<RecommendationDto> recs1 = childService.getRecommendations(child1.getId());
        List<RecommendationDto> recs2 = childService.getRecommendations(child2.getId());
        assertNotNull(recs1);
        assertNotNull(recs2);
        assertFalse(recs1.isEmpty());
        assertFalse(recs2.isEmpty());

        // Child 1 is strong in addition, so Level Up recommendation should be generated
        boolean child1HasLevelUp = recs1.stream().anyMatch(r -> "LEVEL_UP".equals(r.getActionType()));
        assertTrue(child1HasLevelUp, "Child 1 with 100% accuracy should receive Level Up recommendation");

        // Child 2 failed subtraction, so Practice Topic recommendation should be generated
        boolean child2HasPracticeSub = recs2.stream().anyMatch(r -> "subtraction".equalsIgnoreCase(r.getTopic()));
        assertTrue(child2HasPracticeSub, "Child 2 who struggled in subtraction should receive Subtraction recommendation");

        // 8. Verify deleting Child 2 does NOT affect Child 1
        childService.deleteChild(child2.getId());
        assertFalse(childRepository.existsById(child2.getId()));
        assertTrue(childRepository.existsById(child1.getId()));

        ChildProgressResponse prog1After = childService.getChildProgress(child1.getId());
        assertEquals(1, prog1After.getTotalQuizzes(), "Child 1 data must remain completely intact");
        assertEquals(2, prog1After.getTotalQuestions());
        assertEquals(100.0, prog1After.getOverallAccuracy());
    }
}

