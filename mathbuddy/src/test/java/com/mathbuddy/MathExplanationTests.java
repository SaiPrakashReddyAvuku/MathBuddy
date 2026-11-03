package com.mathbuddy;

import com.mathbuddy.dto.AnswerRequest;
import com.mathbuddy.dto.AnswerResponse;
import com.mathbuddy.dto.ExplainRequest;
import com.mathbuddy.dto.ExplainResponse;
import com.mathbuddy.dto.MathExplanationDto;
import com.mathbuddy.dto.StartQuizRequest;
import com.mathbuddy.dto.QuizResponse;
import com.mathbuddy.entity.Child;
import com.mathbuddy.repository.ChildRepository;
import com.mathbuddy.service.MathExplanationBuilder;
import com.mathbuddy.service.QuizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class MathExplanationTests {

    @Autowired
    private QuizService quizService;

    @Autowired
    private ChildRepository childRepository;

    @Autowired
    private MathExplanationBuilder explanationBuilder;

    private Child testChild;

    @BeforeEach
    void setup() {
        testChild = new Child("Sammy", 5, "owl");
        testChild = childRepository.save(testChild);
    }

    @Test
    @DisplayName("Incorrect addition answer produces structured Owl's Math Adventure explanation")
    void testIncorrectAdditionAnswerGeneratesExplanation() {
        StartQuizRequest startReq = new StartQuizRequest();
        startReq.setChildId(testChild.getId());
        startReq.setTopic("addition");
        startReq.setDifficulty("easy");

        QuizResponse quiz = quizService.startQuiz(startReq);

        AnswerRequest ansReq = new AnswerRequest();
        ansReq.setSessionId(quiz.getSessionId());
        ansReq.setQuestion("What is 4 + 2?");
        ansReq.setCorrectAnswer(6.0);
        ansReq.setUserAnswer("5"); // incorrect answer

        AnswerResponse response = quizService.checkAnswer(ansReq);

        assertFalse(response.isCorrect());
        assertEquals(6.0, response.getCorrectAnswer());
        assertNotNull(response.getExplanation(), "Explanation DTO must be present on incorrect answer");

        MathExplanationDto exp = response.getExplanation();
        assertEquals("ADDITION", exp.getOperation());
        assertEquals(6.0, exp.getCorrectAnswer());
        assertNotNull(exp.getSteps());
        assertTrue(exp.getSteps().size() >= 3, "Explanation should have at least 3 steps");
        assertNotNull(exp.getFollowUpQuestion());
    }

    @Test
    @DisplayName("Correct answer does not show error explanation but gives celebration")
    void testCorrectAnswerFlowPreserved() {
        StartQuizRequest startReq = new StartQuizRequest();
        startReq.setChildId(testChild.getId());
        startReq.setTopic("addition");
        startReq.setDifficulty("easy");

        QuizResponse quiz = quizService.startQuiz(startReq);

        AnswerRequest ansReq = new AnswerRequest();
        ansReq.setSessionId(quiz.getSessionId());
        ansReq.setQuestion("What is 4 + 2?");
        ansReq.setCorrectAnswer(6.0);
        ansReq.setUserAnswer("6"); // correct answer

        AnswerResponse response = quizService.checkAnswer(ansReq);

        assertTrue(response.isCorrect());
        assertEquals(6.0, response.getCorrectAnswer());
        assertTrue(response.getScore() > 0);
        assertNotNull(response.getFeedback());
    }

    @Test
    @DisplayName("Subtraction teaching strategy removes items and shows remainder")
    void testSubtractionExplanationStrategy() {
        MathExplanationDto exp = explanationBuilder.buildExplanation("What is 8 - 3?", "4", 5.0, "subtraction");

        assertEquals("SUBTRACTION", exp.getOperation());
        assertEquals(5.0, exp.getCorrectAnswer());
        assertEquals(4, exp.getSteps().size());
        assertEquals("SHOW_GROUP", exp.getSteps().get(0).getVisualType());
        assertEquals("REMOVE_GROUP", exp.getSteps().get(1).getVisualType());
        assertEquals("COUNT_ITEMS", exp.getSteps().get(2).getVisualType());
        assertEquals("EQUATION", exp.getSteps().get(3).getVisualType());
    }

    @Test
    @DisplayName("Counting question type generates sequential star counting explanation")
    void testCountingExplanationStrategy() {
        MathExplanationDto exp = explanationBuilder.buildExplanation("How many stars are in the box?", "3", 5.0, "addition");

        assertEquals("COUNTING", exp.getOperation());
        assertEquals(5.0, exp.getCorrectAnswer());
        assertTrue(exp.getSteps().get(1).getText().contains("1, 2, 3, 4, 5"));
    }

    @Test
    @DisplayName("Comparison question type explains bigger vs smaller")
    void testComparisonExplanationStrategy() {
        MathExplanationDto exp = explanationBuilder.buildExplanation("Which is bigger 4 or 6?", "4", 6.0, "addition");

        assertEquals("COMPARISON", exp.getOperation());
        assertEquals(6.0, exp.getCorrectAnswer());
        assertTrue(exp.getFinalExplanation().contains("bigger"));
    }

    @Test
    @DisplayName("Missing number strategy explains number sequence")
    void testMissingNumberExplanationStrategy() {
        MathExplanationDto exp = explanationBuilder.buildExplanation("1, 2, ___, 4", "5", 3.0, "addition");

        assertEquals("MISSING_NUMBER", exp.getOperation());
        assertEquals(3.0, exp.getCorrectAnswer());
        assertEquals("NUMBER_PATH", exp.getSteps().get(0).getVisualType());
    }

    @Test
    @DisplayName("Explicit /api/quiz/explain endpoint returns rich explanation details")
    void testExplainEndpoint() {
        StartQuizRequest startReq = new StartQuizRequest();
        startReq.setChildId(testChild.getId());
        startReq.setTopic("subtraction");
        startReq.setDifficulty("easy");

        QuizResponse quiz = quizService.startQuiz(startReq);

        ExplainRequest explainReq = new ExplainRequest();
        explainReq.setSessionId(quiz.getSessionId());
        explainReq.setQuestion("What is 7 - 2?");
        explainReq.setCorrectAnswer(5.0);
        explainReq.setUserAnswer("4");
        explainReq.setTopic("subtraction");

        ExplainResponse response = quizService.explain(explainReq);

        assertTrue(response.isSuccess());
        assertNotNull(response.getExplanation());
        assertNotNull(response.getDetails());
        assertEquals(5.0, response.getDetails().getCorrectAnswer());
    }
}

