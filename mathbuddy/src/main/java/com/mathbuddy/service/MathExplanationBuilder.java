package com.mathbuddy.service;

import com.mathbuddy.dto.MathExplanationDto;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic Pedagogical Math Explanation Builder for "Owl's Math Adventure".
 *
 * Implements child-centered teaching strategies for children aged 4-6:
 * - Addition: Two groups of objects combined and counted
 * - Subtraction: Initial group with items removed / crossed out
 * - Counting: Sequential counting with items highlighted one by one
 * - Number comparison: Side-by-side groups
 * - Missing number: Number sequence progression
 * - Multiplication: Equal groups counted
 * - Division: Fair-share distribution among friends
 *
 * All steps are strictly grounded in the backend's verified correctAnswer.
 */
@Component
public class MathExplanationBuilder {

    private static final Pattern BINARY_OP_PATTERN =
            Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*([+\\-−×*÷/])\\s*(\\d+(?:\\.\\d+)?)");

    private static final Pattern COUNTING_PATTERN =
            Pattern.compile("how many\\s+([a-zA-Z]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern COMPARISON_PATTERN =
            Pattern.compile("(?:which is (?:bigger|smaller|more|less)|compare)\\s*(\\d+)\\s*(?:and|or)\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern MISSING_NUM_PATTERN =
            Pattern.compile("(\\d+)[,\\s]+(\\d+)[,\\s]+(?:_+|\\?)[,\\s]+(\\d+)");

    public MathExplanationDto buildExplanation(String question, String userAnswer, double correctAnswer, String topic) {
        String cleanQuestion = question != null ? question.trim() : "";
        String safeTopic = topic != null ? topic.toLowerCase().trim() : "addition";

        // Check for specific question types
        if (cleanQuestion.toLowerCase().contains("how many")) {
            return buildCountingExplanation(cleanQuestion, userAnswer, correctAnswer);
        }
        if (cleanQuestion.toLowerCase().contains("which is") || cleanQuestion.toLowerCase().contains("compare")) {
            return buildComparisonExplanation(cleanQuestion, userAnswer, correctAnswer);
        }
        if (cleanQuestion.contains("___") || cleanQuestion.matches(".*\\d+,\\s*\\d+,\\s*_+.*")) {
            return buildMissingNumberExplanation(cleanQuestion, userAnswer, correctAnswer);
        }

        // Check for standard arithmetic expressions
        Matcher matcher = BINARY_OP_PATTERN.matcher(cleanQuestion);
        if (matcher.find()) {
            double n1 = Double.parseDouble(matcher.group(1));
            String op = matcher.group(2);
            double n2 = Double.parseDouble(matcher.group(3));

            return switch (op) {
                case "+", "addition" -> buildAdditionExplanation(cleanQuestion, userAnswer, (int) n1, (int) n2, (int) correctAnswer);
                case "-", "−", "subtraction" -> buildSubtractionExplanation(cleanQuestion, userAnswer, (int) n1, (int) n2, (int) correctAnswer);
                case "×", "*", "multiplication" -> buildMultiplicationExplanation(cleanQuestion, userAnswer, (int) n1, (int) n2, (int) correctAnswer);
                case "÷", "/", "division" -> buildDivisionExplanation(cleanQuestion, userAnswer, (int) n1, (int) n2, (int) correctAnswer);
                default -> buildGenericExplanation(cleanQuestion, userAnswer, correctAnswer);
            };
        }

        // Fallback by topic if regex didn't match directly
        if ("subtraction".equals(safeTopic)) {
            return buildSubtractionFallback(cleanQuestion, userAnswer, (int) correctAnswer);
        } else if ("multiplication".equals(safeTopic)) {
            return buildMultiplicationFallback(cleanQuestion, userAnswer, (int) correctAnswer);
        } else if ("division".equals(safeTopic)) {
            return buildDivisionFallback(cleanQuestion, userAnswer, (int) correctAnswer);
        }

        return buildAdditionFallback(cleanQuestion, userAnswer, (int) correctAnswer);
    }

    // ── Addition Strategy ─────────────────────────────────────────────────────

    private MathExplanationDto buildAdditionExplanation(String q, String userAns, int num1, int num2, int correct) {
        MathExplanationDto dto = new MathExplanationDto();
        dto.setTitle("Let's Count Apples Together! 🍎");
        dto.setQuestion(q);
        dto.setUserAnswer(userAns);
        dto.setCorrectAnswer(correct);
        dto.setOperation("ADDITION");
        dto.setVisualMeta(new MathExplanationDto.VisualMeta("apple", num1, num2, "+"));

        List<MathExplanationDto.Step> steps = new ArrayList<>();
        steps.add(new MathExplanationDto.Step(1,
                "We start with " + num1 + " delicious red apples.",
                "SHOW_GROUP", num1, 0, num1, "Group 1"));

        steps.add(new MathExplanationDto.Step(2,
                "Our friendly squirrel friend brings " + num2 + " more apples!",
                "ADD_GROUP", num1, num2, num1 + num2, "Group 2 Added"));

        steps.add(new MathExplanationDto.Step(3,
                "Now let's count all the apples together: " + buildCountingString(correct) + "!",
                "COUNT_ITEMS", num1, num2, correct, "Counting All"));

        steps.add(new MathExplanationDto.Step(4,
                num1 + " plus " + num2 + " equals " + correct + "! 🌟",
                "EQUATION", num1, num2, correct, "Solution"));

        dto.setSteps(steps);
        dto.setFinalExplanation(num1 + " + " + num2 + " = " + correct);
        dto.setFollowUpQuestion("Can you count the apples with me to find " + correct + "?");
        return dto;
    }

    // ── Subtraction Strategy ──────────────────────────────────────────────────

    private MathExplanationDto buildSubtractionExplanation(String q, String userAns, int num1, int num2, int correct) {
        MathExplanationDto dto = new MathExplanationDto();
        dto.setTitle("Barnaby's Cookie Sharing Adventure! 🍪");
        dto.setQuestion(q);
        dto.setUserAnswer(userAns);
        dto.setCorrectAnswer(correct);
        dto.setOperation("SUBTRACTION");
        dto.setVisualMeta(new MathExplanationDto.VisualMeta("cookie", num1, num2, "-"));

        List<MathExplanationDto.Step> steps = new ArrayList<>();
        steps.add(new MathExplanationDto.Step(1,
                "We have " + num1 + " warm cookies on our tray.",
                "SHOW_GROUP", num1, 0, num1, "Initial Tray"));

        steps.add(new MathExplanationDto.Step(2,
                num2 + " cookies go to our hungry forest friends. Bye-bye cookies!",
                "REMOVE_GROUP", num1, num2, correct, "Taking Away"));

        steps.add(new MathExplanationDto.Step(3,
                "Let's count how many cookies are left: " + buildCountingString(correct) + "!",
                "COUNT_ITEMS", correct, 0, correct, "Cookies Left"));

        steps.add(new MathExplanationDto.Step(4,
                num1 + " take away " + num2 + " leaves " + correct + "! 🍪✨",
                "EQUATION", num1, num2, correct, "Solution"));

        dto.setSteps(steps);
        dto.setFinalExplanation(num1 + " - " + num2 + " = " + correct);
        dto.setFollowUpQuestion("How many cookies are left for Barnaby?");
        return dto;
    }

    // ── Counting Strategy ─────────────────────────────────────────────────────

    private MathExplanationDto buildCountingExplanation(String q, String userAns, double correct) {
        int count = (int) correct;
        MathExplanationDto dto = new MathExplanationDto();
        dto.setTitle("Let's Count the Stars! ⭐");
        dto.setQuestion(q);
        dto.setUserAnswer(userAns);
        dto.setCorrectAnswer(correct);
        dto.setOperation("COUNTING");
        dto.setVisualMeta(new MathExplanationDto.VisualMeta("star", count, 0, "count"));

        List<MathExplanationDto.Step> steps = new ArrayList<>();
        steps.add(new MathExplanationDto.Step(1,
                "Look at these shining stars in the night sky!",
                "SHOW_GROUP", count, 0, count, "Stars"));

        steps.add(new MathExplanationDto.Step(2,
                "Let's point and count them one by one: " + buildCountingString(count) + "!",
                "COUNT_ITEMS", count, 0, count, "Counting Step"));

        steps.add(new MathExplanationDto.Step(3,
                "There are " + count + " shining stars in all! ⭐",
                "EQUATION", count, 0, count, "Total Stars"));

        dto.setSteps(steps);
        dto.setFinalExplanation("We counted " + count + " stars together!");
        dto.setFollowUpQuestion("Can you say " + count + " with Barnaby?");
        return dto;
    }

    // ── Number Comparison Strategy ────────────────────────────────────────────

    private MathExplanationDto buildComparisonExplanation(String q, String userAns, double correct) {
        Matcher m = COMPARISON_PATTERN.matcher(q);
        int n1 = 4, n2 = 6;
        if (m.find()) {
            n1 = Integer.parseInt(m.group(1));
            n2 = Integer.parseInt(m.group(2));
        }

        int bigger = Math.max(n1, n2);
        int smaller = Math.min(n1, n2);

        MathExplanationDto dto = new MathExplanationDto();
        dto.setTitle("Comparing Animal Friends! 🎈");
        dto.setQuestion(q);
        dto.setUserAnswer(userAns);
        dto.setCorrectAnswer(correct);
        dto.setOperation("COMPARISON");
        dto.setVisualMeta(new MathExplanationDto.VisualMeta("balloon", n1, n2, "vs"));

        List<MathExplanationDto.Step> steps = new ArrayList<>();
        steps.add(new MathExplanationDto.Step(1,
                "Let's look at the two groups! Group A has " + n1 + " balloons. Group B has " + n2 + " balloons.",
                "COMPARE_GROUPS", n1, n2, n1 + n2, "Two Groups"));

        steps.add(new MathExplanationDto.Step(2,
                bigger + " has more balloons than " + smaller + "!",
                "COMPARE_GROUPS", bigger, smaller, bigger, "Comparison"));

        steps.add(new MathExplanationDto.Step(3,
                (int) correct + " is the right answer! 🎈✨",
                "EQUATION", (int) correct, 0, (int) correct, "Result"));

        dto.setSteps(steps);
        dto.setFinalExplanation(bigger + " is bigger than " + smaller + ".");
        dto.setFollowUpQuestion("Which group looks bigger to you?");
        return dto;
    }

    // ── Missing Number Strategy ───────────────────────────────────────────────

    private MathExplanationDto buildMissingNumberExplanation(String q, String userAns, double correct) {
        int missing = (int) correct;
        MathExplanationDto dto = new MathExplanationDto();
        dto.setTitle("Hop Along the Number Trail! 🐰");
        dto.setQuestion(q);
        dto.setUserAnswer(userAns);
        dto.setCorrectAnswer(correct);
        dto.setOperation("MISSING_NUMBER");
        dto.setVisualMeta(new MathExplanationDto.VisualMeta("star", missing, 0, "path"));

        List<MathExplanationDto.Step> steps = new ArrayList<>();
        steps.add(new MathExplanationDto.Step(1,
                "Look at our number steps: let's hop along and count!",
                "NUMBER_PATH", missing - 1, 1, missing + 1, "Stepping Stones"));

        steps.add(new MathExplanationDto.Step(2,
                "What number comes right after " + (missing - 1) + "? Let's hop forward one step!",
                "NUMBER_PATH", missing - 1, 1, missing, "Next Step"));

        steps.add(new MathExplanationDto.Step(3,
                "It's " + missing + "! The missing number is " + missing + "! 🌟",
                "EQUATION", missing, 0, missing, "Answer"));

        dto.setSteps(steps);
        dto.setFinalExplanation("The missing number in the line is " + missing + ".");
        dto.setFollowUpQuestion("Can you hop and say " + missing + "?");
        return dto;
    }

    // ── Multiplication Strategy ───────────────────────────────────────────────

    private MathExplanationDto buildMultiplicationExplanation(String q, String userAns, int num1, int num2, int correct) {
        MathExplanationDto dto = new MathExplanationDto();
        dto.setTitle("Grouping Sweet Berries! 🍓");
        dto.setQuestion(q);
        dto.setUserAnswer(userAns);
        dto.setCorrectAnswer(correct);
        dto.setOperation("MULTIPLICATION");
        dto.setVisualMeta(new MathExplanationDto.VisualMeta("cookie", num1, num2, "×"));

        List<MathExplanationDto.Step> steps = new ArrayList<>();
        steps.add(new MathExplanationDto.Step(1,
                "We have " + num1 + " friendly baskets. Each basket holds " + num2 + " sweet berries.",
                "SHOW_GROUP", num1, num2, correct, "Equal Groups"));

        steps.add(new MathExplanationDto.Step(2,
                "Multiplication means adding equal groups: " + num1 + " groups of " + num2 + "!",
                "COUNT_ITEMS", num1, num2, correct, "Grouping"));

        steps.add(new MathExplanationDto.Step(3,
                num1 + " times " + num2 + " equals " + correct + "! 🍓✨",
                "EQUATION", num1, num2, correct, "Total"));

        dto.setSteps(steps);
        dto.setFinalExplanation(num1 + " × " + num2 + " = " + correct);
        dto.setFollowUpQuestion("Can you count the groups with me?");
        return dto;
    }

    // ── Division Strategy ─────────────────────────────────────────────────────

    private MathExplanationDto buildDivisionExplanation(String q, String userAns, int num1, int num2, int correct) {
        MathExplanationDto dto = new MathExplanationDto();
        dto.setTitle("Sharing Fairly with Friends! 🎈");
        dto.setQuestion(q);
        dto.setUserAnswer(userAns);
        dto.setCorrectAnswer(correct);
        dto.setOperation("DIVISION");
        dto.setVisualMeta(new MathExplanationDto.VisualMeta("balloon", num1, num2, "÷"));

        List<MathExplanationDto.Step> steps = new ArrayList<>();
        steps.add(new MathExplanationDto.Step(1,
                "We have " + num1 + " colorful balloons to share among " + num2 + " friends.",
                "SHOW_GROUP", num1, num2, num1, "All Balloons"));

        steps.add(new MathExplanationDto.Step(2,
                "When we share them equally, each friend gets " + correct + " balloons!",
                "REMOVE_GROUP", num1, num2, correct, "Equal Sharing"));

        steps.add(new MathExplanationDto.Step(3,
                num1 + " divided by " + num2 + " is " + correct + "! 🎈✨",
                "EQUATION", num1, num2, correct, "Fair Share"));

        dto.setSteps(steps);
        dto.setFinalExplanation(num1 + " ÷ " + num2 + " = " + correct);
        dto.setFollowUpQuestion("Does every friend have " + correct + " balloons?");
        return dto;
    }

    // ── Generic Fallbacks ─────────────────────────────────────────────────────

    private MathExplanationDto buildAdditionFallback(String q, String userAns, int correct) {
        int n1 = Math.max(1, correct / 2);
        int n2 = correct - n1;
        return buildAdditionExplanation(q, userAns, n1, n2, correct);
    }

    private MathExplanationDto buildSubtractionFallback(String q, String userAns, int correct) {
        int n1 = correct + 2;
        int n2 = 2;
        return buildSubtractionExplanation(q, userAns, n1, n2, correct);
    }

    private MathExplanationDto buildMultiplicationFallback(String q, String userAns, int correct) {
        return buildMultiplicationExplanation(q, userAns, 2, Math.max(1, correct / 2), correct);
    }

    private MathExplanationDto buildDivisionFallback(String q, String userAns, int correct) {
        return buildDivisionExplanation(q, userAns, correct * 2, 2, correct);
    }

    private MathExplanationDto buildGenericExplanation(String q, String userAns, double correct) {
        int c = (int) correct;
        MathExplanationDto dto = new MathExplanationDto();
        dto.setTitle("Let's Solve This Step-by-Step! 💡");
        dto.setQuestion(q);
        dto.setUserAnswer(userAns);
        dto.setCorrectAnswer(correct);
        dto.setOperation("GENERAL");
        dto.setVisualMeta(new MathExplanationDto.VisualMeta("apple", c, 0, "="));

        List<MathExplanationDto.Step> steps = new ArrayList<>();
        steps.add(new MathExplanationDto.Step(1,
                "Let's look at the question together: " + q,
                "SHOW_GROUP", c, 0, c, "Question"));

        steps.add(new MathExplanationDto.Step(2,
                "When we count and calculate carefully, we find the answer is " + c + "!",
                "COUNT_ITEMS", c, 0, c, "Counting"));

        steps.add(new MathExplanationDto.Step(3,
                "Great effort! The correct answer is " + c + ". 🌟",
                "EQUATION", c, 0, c, "Answer"));

        dto.setSteps(steps);
        dto.setFinalExplanation("The answer to " + q + " is " + c + ".");
        dto.setFollowUpQuestion("Would you like to try this one again?");
        return dto;
    }

    private String buildCountingString(int count) {
        if (count <= 0) return "0";
        if (count > 10) return "1, 2, 3... all the way to " + count;
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            sb.append(i);
            if (i < count) sb.append(", ");
        }
        return sb.toString();
    }
}

