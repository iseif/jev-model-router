package dev.iseif.jevmodelrouter.model;

import java.util.Map;

public record AnswerReview(
    double groundedProbability,
    double completenessScore,
    double completenessConfidence,
    Map<Integer, Double> completenessProbabilities,
    ReviewOutcome outcome,
    String judgeModel,
    CallMetrics jev) {

  public AnswerReview {
    completenessProbabilities = Map.copyOf(completenessProbabilities);
  }
}
