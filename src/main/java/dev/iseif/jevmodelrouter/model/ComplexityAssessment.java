package dev.iseif.jevmodelrouter.model;

import java.util.Map;

public record ComplexityAssessment(TaskComplexity predicted, double confidence,
    Map<TaskComplexity, Double> probabilities, String judgeModel, CallMetrics jev) {

  public ComplexityAssessment {
    probabilities = Map.copyOf(probabilities);
  }
}
