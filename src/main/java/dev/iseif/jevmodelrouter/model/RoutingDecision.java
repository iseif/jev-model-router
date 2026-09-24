package dev.iseif.jevmodelrouter.model;

import java.util.Map;

/** The prediction stays visible even when the application chooses a fallback. */
public record RoutingDecision(
    TaskComplexity predictedComplexity,
    TaskComplexity selectedComplexity,
    String modelId,
    double confidence,
    Map<TaskComplexity, Double> probabilities,
    RoutingReason reason,
    String judgeModel,
    CallMetrics jev) {

  public RoutingDecision {
    probabilities = Map.copyOf(probabilities);
  }
}
