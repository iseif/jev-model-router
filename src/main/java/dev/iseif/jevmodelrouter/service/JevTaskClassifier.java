package dev.iseif.jevmodelrouter.service;

import dev.iseif.jevmodelrouter.model.CallMetrics;
import dev.iseif.jevmodelrouter.model.ComplexityAssessment;
import dev.iseif.jevmodelrouter.model.TaskComplexity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.question.Choice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class JevTaskClassifier {

  private static final String QUESTION = "complexity";
  private static final Set<String> LABELS = Arrays.stream(TaskComplexity.values())
      .map(Enum::name).collect(Collectors.toUnmodifiableSet());

  private final TypeSafeClient client;
  private final ProviderCallTimer timer;
  private final Choice complexityQuestion;

  public JevTaskClassifier(TypeSafeClient client, ProviderCallTimer timer,
      @Value("classpath:prompts/routing-instructions.txt") Resource instructions) throws IOException {
    this.client = client;
    this.timer = timer;
    var builder = Choice.builder().instructions(instructions.getContentAsString(StandardCharsets.UTF_8));
    for (TaskComplexity complexity : TaskComplexity.values()) {
      builder.option(complexity.name(), complexity.description());
    }
    this.complexityQuestion = builder.build();
  }

  public ComplexityAssessment classify(String prompt) {
    var call = timer.measure(ProviderOperation.JEV_CLASSIFICATION, client.defaultModel(),
        () -> JevCalls.call(() -> client.systemOne(
            Map.of("prompt", prompt), Map.of(QUESTION, complexityQuestion))));
    var response = call.value();
    var answer = JevCalls.call(() -> response.choice(QUESTION));
    JevResponseValidator.choice(answer, LABELS);

    var probabilities = new EnumMap<TaskComplexity, Double>(TaskComplexity.class);
    answer.probabilities().forEach((label, probability) -> probabilities.put(TaskComplexity.valueOf(label), probability));
    var usage = response.usage();
    return new ComplexityAssessment(TaskComplexity.valueOf(answer.value()), answer.confidence(),
        probabilities, response.model(),
        new CallMetrics(call.durationMs(), usage.inputTokens(), usage.outputTokens()));
  }
}
