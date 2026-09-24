package dev.iseif.jevmodelrouter.config;

import dev.iseif.jevmodelrouter.model.TaskComplexity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AiPropertiesTests {

  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(PropertiesConfiguration.class)
      .withPropertyValues(
          "routing.min-confidence=0.7",
          "routing.low-confidence-fallback=DEMANDING",
          "routing.models.routine=small", "routing.models.standard=medium",
          "routing.models.complex=large", "routing.models.demanding=largest",
          "answer-review.min-grounded-probability=0.85",
          "answer-review.min-completeness-score=1.5",
          "answer-review.min-completeness-confidence=0.7");

  @ParameterizedTest
  @ValueSource(strings = {
      "routing.min-confidence=-0.1", "routing.min-confidence=1.01", "routing.min-confidence=NaN",
      "routing.models.routine=", "routing.models.demanding= ",
      "routing.low-confidence-fallback=", "routing.low-confidence-fallback=UNKNOWN",
      "answer-review.min-grounded-probability=1.1", "answer-review.min-completeness-score=2.1",
      "answer-review.min-completeness-confidence=-0.1"
  })
  void rejectsConfigurationThatCannotRepresentThePolicy(String invalidProperty) {
    runner.withPropertyValues(invalidProperty).run(context -> assertThat(context).hasFailed());
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.0", "1.0"})
  void acceptsBothEndsOfTheConfidenceRange(String value) {
    runner.withPropertyValues("routing.min-confidence=" + value)
        .run(context -> assertThat(context).hasNotFailed());
  }

  @Test
  void bindsTheConfiguredFallbackCategory() {
    runner.withPropertyValues("routing.low-confidence-fallback=COMPLEX").run(context -> {
      assertThat(context).hasNotFailed();
      assertThat(context.getBean(ModelRoutingProperties.class).lowConfidenceFallback())
          .isEqualTo(TaskComplexity.COMPLEX);
    });
  }

  @Configuration(proxyBeanMethods = false)
  @EnableConfigurationProperties({ModelRoutingProperties.class, AnswerReviewProperties.class})
  static class PropertiesConfiguration {}
}
