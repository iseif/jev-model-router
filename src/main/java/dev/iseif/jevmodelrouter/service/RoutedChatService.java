package dev.iseif.jevmodelrouter.service;

import dev.iseif.jevmodelrouter.model.RoutedChatResponse;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;

@Service
public class RoutedChatService {

  private final ModelRouter router;
  private final AnswerGenerator generator;

  public RoutedChatService(ModelRouter router, AnswerGenerator generator) {
    this.router = router;
    this.generator = generator;
  }

  public RoutedChatResponse chat(String prompt) {
    long started = System.nanoTime();
    var decision = router.route(prompt);
    var answer = generator.generate(prompt, decision.modelId());
    long totalDurationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
    return new RoutedChatResponse(decision, answer.content(), answer.modelId(), answer.metrics(), totalDurationMs);
  }
}
