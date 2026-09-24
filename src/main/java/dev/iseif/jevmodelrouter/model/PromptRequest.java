package dev.iseif.jevmodelrouter.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromptRequest(@NotBlank @Size(max = 12000) String prompt) {
}
