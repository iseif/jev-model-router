package dev.iseif.jevmodelrouter.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnswerReviewRequest(
    @NotBlank @Size(max = 4000) String question,
    @NotBlank @Size(max = 12000) String reference,
    @NotBlank @Size(max = 12000) String answer) {}
