package dev.iseif.jevmodelrouter.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("answer-review")
public record AnswerReviewProperties(
    @DecimalMin("0.0") @DecimalMax("1.0") double minGroundedProbability,
    @DecimalMin("0.0") @DecimalMax("2.0") double minCompletenessScore,
    @DecimalMin("0.0") @DecimalMax("1.0") double minCompletenessConfidence) {}
