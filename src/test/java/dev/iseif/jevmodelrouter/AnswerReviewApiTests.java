package dev.iseif.jevmodelrouter;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnswerReviewApiTests extends ProviderApiTest {

  @ParameterizedTest
  @CsvSource({"supported,0.98,1.99,PASS", "contradictory,0.01,0.75,NEEDS_REVIEW", "incomplete,0.6,0.62,NEEDS_REVIEW"})
  void replaysRecordedJevReviews(String example, double grounded, double score, String outcome) throws Exception {
    PROVIDER.jev(200, fixture("review-" + example));
    var answer = switch (example) {
      case "supported" -> "You may return an unopened item within 30 days if you have a receipt.";
      case "contradictory" -> "You have 90 days, and no receipt is needed.";
      default -> "You can return it.";
    };
    var request = Map.of("question", "When can I return an unopened item?",
        "reference", "Unopened items may be returned within 30 days. A receipt is required.", "answer", answer);
    mvc.perform(post("/answer-reviews").contentType("application/json").content(JSON.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groundedProbability").value(grounded))
        .andExpect(jsonPath("$.completenessScore").value(score))
        .andExpect(jsonPath("$.outcome").value(outcome));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"grounded\":{\"type\":\"choice\",\"choice\":\"PASS\",\"confidence\":1.0,\"probabilities\":{\"PASS\":1.0}}}"})
  void rejectsMissingOrWrongAnswerTypesEvenWhenAJudgeReturnsAVerdict(String answers) throws Exception {
    var body = (ObjectNode) JSON.readTree(fixture("review-supported"));
    body.set("answers", JSON.readTree(answers));
    PROVIDER.jev(200, body.toString());
    mvc.perform(post("/answer-reviews").contentType("application/json").content(reviewRequest()))
        .andExpect(status().isBadGateway());
  }

  @ParameterizedTest
  @CsvSource({"0.90,1.8,0.90,PASS", "0.85,1.5,0.70,PASS", "0.84,1.8,0.90,NEEDS_REVIEW", "0.90,1.4,0.90,NEEDS_REVIEW", "0.90,1.8,0.69,NEEDS_REVIEW"})
  void reviewsGroundingAndCompletenessIndependently(double grounded, double score, double confidence, String outcome) throws Exception {
    PROVIDER.jev(200, assessment(grounded, score, confidence));

    mvc.perform(post("/answer-reviews").contentType("application/json").content(reviewRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.groundedProbability").value(grounded))
        .andExpect(jsonPath("$.completenessScore").value(score))
        .andExpect(jsonPath("$.completenessConfidence").value(confidence))
        .andExpect(jsonPath("$.outcome").value(outcome));

    assertThat(PROVIDER.jevRequests()).hasSize(1);
    assertThat(PROVIDER.chatRequests()).isEmpty();
    var sent = JSON.readTree(PROVIDER.jevRequests().getFirst());
    assertThat(sent.at("/state/reference").stringValue()).isEqualTo("Unopened items may be returned within 30 days.");
    assertThat(sent.at("/questions/grounded/type").stringValue()).isEqualTo("noul");
    assertThat(sent.at("/questions/completeness/type").stringValue()).isEqualTo("score");
    assertThat(sent.get("questions").size()).isEqualTo(2);
  }

  @ParameterizedTest
  @ValueSource(strings = {"question", "reference", "answer"})
  void requiresEveryAnswerReviewField(String missingField) throws Exception {
    var body = JSON.readTree(reviewRequest()).deepCopy();
    ((ObjectNode) body).remove(missingField);
    mvc.perform(post("/answer-reviews").contentType("application/json").content(body.toString()))
        .andExpect(status().isBadRequest());
    assertThat(PROVIDER.jevRequests()).isEmpty();
  }

  @ParameterizedTest
  @CsvSource({"question,4001", "reference,12001", "answer,12001"})
  void boundsEveryAnswerReviewField(String field, int length) throws Exception {
    var body = (ObjectNode) JSON.readTree(reviewRequest());
    body.put(field, "x".repeat(length));
    mvc.perform(post("/answer-reviews").contentType("application/json").content(body.toString()))
        .andExpect(status().isBadRequest());
    assertThat(PROVIDER.jevRequests()).isEmpty();
  }

  @Test
  void acceptsAScoreRoundedIndependentlyOfItsProbabilities() throws Exception {
    var body = (ObjectNode) JSON.readTree(assessment(0.95, 1.433, 0.9));
    ((ObjectNode) body.at("/answers/completeness")).put("score", 1.43);
    PROVIDER.jev(200, body.toString());
    mvc.perform(post("/answer-reviews").contentType("application/json").content(reviewRequest()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.completenessScore").value(1.43))
        .andExpect(jsonPath("$.jev.inputTokens").value(150));
  }

  @Test
  void rejectsAnInconsistentScoreInsteadOfPassingTheReview() throws Exception {
    var body = (ObjectNode) JSON.readTree(assessment(0.95, 1.8, 0.9));
    ((ObjectNode) body.at("/answers/completeness")).put("score", 2.0);
    PROVIDER.jev(200, body.toString());
    mvc.perform(post("/answer-reviews").contentType("application/json").content(reviewRequest()))
        .andExpect(status().isBadGateway());
  }

}
