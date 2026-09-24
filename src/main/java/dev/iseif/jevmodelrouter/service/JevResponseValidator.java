package dev.iseif.jevmodelrouter.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springaicommunity.typesafe.question.Score;
import org.springaicommunity.typesafe.response.ChoiceAnswer;
import org.springaicommunity.typesafe.response.ScoreAnswer;

/** Validate the application's rubric while allowing independently rounded provider numbers. */
final class JevResponseValidator {

  // Allow each returned number to have been independently rounded to two decimal places.
  private static final double MAX_ROUNDING_ERROR = 0.005;
  private static final double FLOATING_POINT_EPSILON = 1e-12;

  private JevResponseValidator() {}

  static void choice(ChoiceAnswer answer, Set<String> labels) {
    if (answer.value() == null || !labels.contains(answer.value())) {
      throw AiUpstreamException.invalidResponse();
    }
    range(answer.confidence(), 1.0);
    distribution(answer.probabilities(), labels);
    double winner = answer.probabilities().get(answer.value());
    if (answer.probabilities().values().stream().anyMatch(value -> value > winner)) {
      throw AiUpstreamException.invalidResponse();
    }
  }

  static void score(ScoreAnswer answer, Score rubric) {
    int maxLevel = rubric.maxLevel();
    var levels = IntStream.rangeClosed(0, maxLevel).boxed().collect(Collectors.toUnmodifiableSet());
    range(answer.value(), maxLevel);
    range(answer.confidence(), 1.0);
    distribution(answer.probabilities(), levels);
    if (!answer.legend().keySet().equals(levels)) {
      throw AiUpstreamException.invalidResponse();
    }
    double expected = answer.probabilities().entrySet().stream()
        .mapToDouble(entry -> entry.getKey() * entry.getValue()).sum();
    double scoreTolerance = MAX_ROUNDING_ERROR * (1 + levels.stream().mapToInt(Integer::intValue).sum());
    if (Math.abs(answer.value() - expected) > scoreTolerance + FLOATING_POINT_EPSILON) {
      throw AiUpstreamException.invalidResponse();
    }
  }

  static void range(double value, double max) {
    if (!Double.isFinite(value) || value < 0.0 || value > max) {
      throw AiUpstreamException.invalidResponse();
    }
  }

  static <K> void distribution(Map<K, Double> probabilities, Set<K> labels) {
    if (!probabilities.keySet().equals(labels)) {
      throw AiUpstreamException.invalidResponse();
    }
    double sum = 0;
    for (Double probability : probabilities.values()) {
      if (probability == null) {
        throw AiUpstreamException.invalidResponse();
      }
      range(probability, 1.0);
      sum += probability;
    }
    if (Math.abs(sum - 1.0) > MAX_ROUNDING_ERROR * labels.size() + FLOATING_POINT_EPSILON) {
      throw AiUpstreamException.invalidResponse();
    }
  }
}
