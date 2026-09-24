package dev.iseif.jevmodelrouter.config;

import dev.iseif.jevmodelrouter.model.TaskComplexity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("routing")
public record ModelRoutingProperties(
    @DecimalMin("0.0") @DecimalMax("1.0") double minConfidence,
    // Minimum task category when confidence is low; higher predictions are retained.
    @NotNull TaskComplexity lowConfidenceFallback,
    @NotNull @Valid Models models) {

  public String modelFor(TaskComplexity complexity) {
    return switch (complexity) {
      case ROUTINE -> models.routine();
      case STANDARD -> models.standard();
      case COMPLEX -> models.complex();
      case DEMANDING -> models.demanding();
    };
  }

  public record Models(
      @NotBlank String routine,
      @NotBlank String standard,
      @NotBlank String complex,
      @NotBlank String demanding) {}
}
