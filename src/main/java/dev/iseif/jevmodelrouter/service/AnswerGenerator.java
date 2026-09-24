package dev.iseif.jevmodelrouter.service;

import dev.iseif.jevmodelrouter.model.GeneratedAnswer;

public interface AnswerGenerator {
  GeneratedAnswer generate(String prompt, String modelId);
}
