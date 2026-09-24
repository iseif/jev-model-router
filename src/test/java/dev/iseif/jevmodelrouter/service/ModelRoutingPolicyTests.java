package dev.iseif.jevmodelrouter.service;

import dev.iseif.jevmodelrouter.config.ModelRoutingProperties;
import dev.iseif.jevmodelrouter.model.CallMetrics;
import dev.iseif.jevmodelrouter.model.ComplexityAssessment;
import dev.iseif.jevmodelrouter.model.RoutingReason;
import dev.iseif.jevmodelrouter.model.TaskComplexity;
import java.util.EnumMap;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ModelRoutingPolicyTests {

  private static final ModelRoutingProperties.Models MODELS =
      new ModelRoutingProperties.Models("small", "medium", "large", "largest");
  private final ModelRoutingPolicy policy = new ModelRoutingPolicy(
      new ModelRoutingProperties(0.70, TaskComplexity.DEMANDING, MODELS));

  @ParameterizedTest
  @CsvSource({"0.6999,DEMANDING,largest,LOW_CONFIDENCE_FALLBACK", "0.70,STANDARD,medium,CLASSIFIED", "1.0,STANDARD,medium,CLASSIFIED"})
  void appliesTheBoundaryWithoutRewritingThePrediction(double confidence, TaskComplexity selected,
      String model, RoutingReason reason) {
    var assessment = new ComplexityAssessment(TaskComplexity.STANDARD, confidence,
        Map.of(TaskComplexity.ROUTINE, 0.1, TaskComplexity.STANDARD, 0.8,
            TaskComplexity.COMPLEX, 0.1, TaskComplexity.DEMANDING, 0.0),
        "jev-test", new CallMetrics(250, 600, 50));

    var decision = policy.decide(assessment);
    assertThat(decision.predictedComplexity()).isEqualTo(TaskComplexity.STANDARD);
    assertThat(decision.selectedComplexity()).isEqualTo(selected);
    assertThat(decision.modelId()).isEqualTo(model);
    assertThat(decision.reason()).isEqualTo(reason);
    assertThat(decision.confidence()).isEqualTo(confidence);
    assertThat(decision.probabilities()).isEqualTo(assessment.probabilities());
    assertThat(decision.jev()).isSameAs(assessment.jev());
  }

  @ParameterizedTest
  @CsvSource({
      "STANDARD,0.69,COMPLEX,large,LOW_CONFIDENCE_FALLBACK",
      "COMPLEX,0.69,COMPLEX,large,LOW_CONFIDENCE_FALLBACK",
      "DEMANDING,0.68,DEMANDING,largest,LOW_CONFIDENCE_FALLBACK",
      "ROUTINE,0.70,ROUTINE,small,CLASSIFIED"
  })
  void usesTheConfiguredFallbackAsAMinimumOnlyWhenUncertain(TaskComplexity predicted,
      double confidence, TaskComplexity selected, String model, RoutingReason reason) {
    var configuredPolicy = new ModelRoutingPolicy(new ModelRoutingProperties(0.70, TaskComplexity.COMPLEX, MODELS));
    var probabilities = new EnumMap<TaskComplexity, Double>(TaskComplexity.class);
    for (var category : TaskComplexity.values()) {
      probabilities.put(category, category == predicted ? 0.76 : 0.08);
    }
    var assessment = new ComplexityAssessment(predicted, confidence, probabilities,
        "jev-test", new CallMetrics(100, 600, 50));

    var decision = configuredPolicy.decide(assessment);

    assertThat(decision.predictedComplexity()).isEqualTo(predicted);
    assertThat(decision.selectedComplexity()).isEqualTo(selected);
    assertThat(decision.modelId()).isEqualTo(model);
    assertThat(decision.reason()).isEqualTo(reason);
    assertThat(decision.confidence()).isEqualTo(confidence);
    assertThat(decision.probabilities()).isEqualTo(assessment.probabilities());
    assertThat(decision.jev()).isSameAs(assessment.jev());
  }
}
