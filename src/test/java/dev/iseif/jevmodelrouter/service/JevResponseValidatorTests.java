package dev.iseif.jevmodelrouter.service;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springaicommunity.typesafe.JsonContent;
import org.springaicommunity.typesafe.question.Score;
import org.springaicommunity.typesafe.response.ScoreAnswer;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JevResponseValidatorTests {

  @ParameterizedTest
  @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -0.01, 1.01})
  void rejectsNonFiniteAndOutOfRangeProbabilities(double value) {
    assertThatThrownBy(() -> JevResponseValidator.range(value, 1.0)).isInstanceOf(AiUpstreamException.class);
  }

  @Test
  void allowsSmallRoundingDifferencesButNotAnUnnormalizedDistribution() {
    assertThatCode(() -> JevResponseValidator.distribution(Map.of(0, 0.333, 1, 0.333, 2, 0.333), Set.of(0, 1, 2)))
        .doesNotThrowAnyException();
    assertThatThrownBy(() -> JevResponseValidator.distribution(Map.of(0, 0.9, 1, 0.9), Set.of(0, 1)))
        .isInstanceOf(AiUpstreamException.class);
  }

  @Test
  void acceptsFourOptionsRoundedIndependentlyInEitherDirection() {
    // Both represent the normalized distribution [0.125, 0.125, 0.125, 0.625].
    var labels = Set.of(0, 1, 2, 3);
    assertThatCode(() -> JevResponseValidator.distribution(Map.of(0, 0.12, 1, 0.12, 2, 0.12, 3, 0.62), labels))
        .doesNotThrowAnyException();
    assertThatCode(() -> JevResponseValidator.distribution(Map.of(0, 0.13, 1, 0.13, 2, 0.13, 3, 0.63), labels))
        .doesNotThrowAnyException();
  }

  @Test
  void validatesAgainstTheRequestedRubricInsteadOfTrustingTheReturnedLegend() {
    var rubric = Score.of("Completeness", "Missing", "Partial", "Complete");
    var answer = new ScoreAnswer(3.0,
        Map.of(0, JsonContent.of("Missing"), 1, JsonContent.of("Partial"),
            2, JsonContent.of("Complete"), 3, JsonContent.of("Unexpected")),
        Map.of(0, 0.0, 1, 0.0, 2, 0.0, 3, 1.0), 1.0);
    assertThatThrownBy(() -> JevResponseValidator.score(answer, rubric))
        .isInstanceOf(AiUpstreamException.class);
  }
}
