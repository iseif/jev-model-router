package dev.iseif.jevmodelrouter.service;

import dev.iseif.jevmodelrouter.config.AnswerReviewProperties;
import dev.iseif.jevmodelrouter.model.AnswerReview;
import dev.iseif.jevmodelrouter.model.AnswerReviewRequest;
import dev.iseif.jevmodelrouter.model.CallMetrics;
import dev.iseif.jevmodelrouter.model.ReviewOutcome;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springaicommunity.typesafe.JsonContent;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.judge.JevJudge;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.question.Score;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class JevAnswerReviewer {

  private final TypeSafeClient client;
  private final ProviderCallTimer timer;
  private final JevJudge judge;
  private final Score completenessRubric;

  public JevAnswerReviewer(TypeSafeClient client, AnswerReviewProperties properties, ProviderCallTimer timer,
      @Value("classpath:prompts/grounding-instructions.txt") Resource groundingInstructions,
      @Value("classpath:prompts/completeness-instructions.txt") Resource completenessInstructions) throws IOException {
    this.client = client;
    this.timer = timer;
    var grounding = Noul.builder()
        .instructions(groundingInstructions.getContentAsString(StandardCharsets.UTF_8))
        .whenTrue("Every factual claim in the answer is supported by the reference, with no contradictions or invented details.")
        .whenFalse("At least one factual claim contradicts the reference or adds a detail that the reference does not support.")
        .build();
    this.completenessRubric = Score.builder()
        .instructions(completenessInstructions.getContentAsString(StandardCharsets.UTF_8))
        .level("The answer does not address the question, or omits the information needed to respond to it.")
        .level("The answer addresses part of the question but omits a requested detail or a necessary qualification from the reference.")
        .level("The answer addresses every part that the reference can resolve, including necessary qualifications; it identifies any requested facts the reference does not supply.")
        .build();
    this.judge = JevJudge.builder(client)
        .noul("grounded", grounding, properties.minGroundedProbability())
        .score("completeness", completenessRubric, properties.minCompletenessScore())
        .minConfidence(properties.minCompletenessConfidence())
        .failOnInconclusive(true)
        .build();
  }

  public AnswerReview review(AnswerReviewRequest request) {
    var state = JsonContent.of(Map.of("question", request.question(),
        "reference", request.reference(), "answer", request.answer()));
    var call = timer.measure(ProviderOperation.JEV_ANSWER_REVIEW, client.defaultModel(),
        () -> JevCalls.call(() -> judge.judge(state)));
    var verdict = call.value();
    var response = verdict.response();
    double grounded = JevCalls.call(() -> response.noulValue("grounded"));
    var completeness = JevCalls.call(() -> response.score("completeness"));
    JevResponseValidator.range(grounded, 1.0);
    JevResponseValidator.score(completeness, completenessRubric);

    var usage = response.usage();
    return new AnswerReview(grounded, completeness.value(), completeness.confidence(),
        completeness.probabilities(), verdict.passed() ? ReviewOutcome.PASS : ReviewOutcome.NEEDS_REVIEW,
        response.model(), new CallMetrics(call.durationMs(), usage.inputTokens(), usage.outputTokens()));
  }
}
