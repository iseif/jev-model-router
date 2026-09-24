package dev.iseif.jevmodelrouter.service;

import dev.iseif.jevmodelrouter.config.ModelRoutingProperties;
import dev.iseif.jevmodelrouter.model.ComplexityAssessment;
import dev.iseif.jevmodelrouter.model.RoutingDecision;
import dev.iseif.jevmodelrouter.model.RoutingReason;
import dev.iseif.jevmodelrouter.model.TaskComplexity;
import org.springframework.stereotype.Component;

/** The application owns this policy; it neither calls nor depends on an AI SDK. */
@Component
public class ModelRoutingPolicy {

  private final ModelRoutingProperties properties;

  public ModelRoutingPolicy(ModelRoutingProperties properties) {
    this.properties = properties;
  }

  public RoutingDecision decide(ComplexityAssessment assessment) {
    boolean uncertain = assessment.confidence() < properties.minConfidence();
    var predicted = assessment.predicted();
    var fallback = properties.lowConfidenceFallback();
    var selected = uncertain && rank(fallback) > rank(predicted) ? fallback : predicted;
    var reason = uncertain ? RoutingReason.LOW_CONFIDENCE_FALLBACK : RoutingReason.CLASSIFIED;
    return new RoutingDecision(assessment.predicted(), selected, properties.modelFor(selected),
        assessment.confidence(), assessment.probabilities(), reason, assessment.judgeModel(), assessment.jev());
  }

  // Category difficulty is an explicit policy, independent of enum declaration order.
  private static int rank(TaskComplexity complexity) {
    return switch (complexity) {
      case ROUTINE -> 0;
      case STANDARD -> 1;
      case COMPLEX -> 2;
      case DEMANDING -> 3;
    };
  }
}
