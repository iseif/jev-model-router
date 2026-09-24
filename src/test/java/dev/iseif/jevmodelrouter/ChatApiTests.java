package dev.iseif.jevmodelrouter;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ChatApiTests extends ProviderApiTest {

  @Test
  @ExtendWith(OutputCaptureExtension.class)
  void doesNotLogThePromptWhenOpenAiReturnsNoChoices(CapturedOutput output) throws Exception {
    PROVIDER.jev(200, choice("ROUTINE", 0.9));
    PROVIDER.chat(200, """
        {"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"test-routine","choices":[]}
        """);
    String prompt = "private-prompt-sentinel";

    mvc.perform(post("/chat").contentType("application/json")
            .content(JSON.writeValueAsString(Map.of("prompt", prompt))))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("AI_RESPONSE_INVALID"));

    assertThat(output).doesNotContain(prompt);
    assertThat(output).contains("provider=openai", "operation=generation");
  }

  @Test
  void keepsMissingOpenAiUsageUnknown() throws Exception {
    PROVIDER.jev(200, choice("ROUTINE", 0.9));
    PROVIDER.chat(200, """
        {"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"test-routine",
         "choices":[{"index":0,"message":{"role":"assistant","content":"Hello."},"finish_reason":"stop"}]}
        """);
    mvc.perform(post("/chat").contentType("application/json").content("{\"prompt\":\"Hello\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.generation.inputTokens").value(nullValue()))
        .andExpect(jsonPath("$.generation.outputTokens").value(nullValue()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"prompt_tokens", "completion_tokens", "total_tokens"})
  void rejectsTokenCountsThatOverflowTheSpringAiUsageType(String field) throws Exception {
    PROVIDER.jev(200, choice("ROUTINE", 0.9));
    var response = JSON.readTree("""
        {"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"test-routine",
         "choices":[{"index":0,"message":{"role":"assistant","content":"Hello."},"finish_reason":"stop"}],
         "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
        """);
    ((ObjectNode) response.get("usage")).put(field, 2_147_483_648L);
    PROVIDER.chat(200, response.toString());

    mvc.perform(post("/chat").contentType("application/json").content("{\"prompt\":\"Hello\"}"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("AI_RESPONSE_INVALID"));
    assertThat(PROVIDER.chatRequests()).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"{}", "{\"prompt\":null}", "{\"prompt\":\"\"}", "{\"prompt\":\"   \"}"})
  void rejectsInvalidPromptsBeforeCallingEitherProvider(String body) throws Exception {
    mvc.perform(post("/chat").contentType("application/json").content(body))
        .andExpect(status().isBadRequest());

    assertThat(PROVIDER.jevRequests()).isEmpty();
    assertThat(PROVIDER.chatRequests()).isEmpty();
  }

  @Test
  void sendsTheSelectedModelAndTheUnmodifiedPromptToOpenAi() throws Exception {
    PROVIDER.jev(200, choice("STANDARD", 0.9));
    PROVIDER.chat(200, """
        {"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"test-standard",
         "choices":[{"index":0,"message":{"role":"assistant","content":"A record carries data."},"finish_reason":"stop"}],
         "usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15}}
        """);
    String prompt = "Explain {records} with a literal $variable.\nKeep it brief.";

    mvc.perform(post("/chat").contentType("application/json").content(JSON.writeValueAsString(Map.of("prompt", prompt))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.routingDecision.modelId").value("test-standard"))
        .andExpect(jsonPath("$.routingDecision.jev.durationMs").isNumber())
        .andExpect(jsonPath("$.routingDecision.jev.inputTokens").value(100))
        .andExpect(jsonPath("$.generation.durationMs").isNumber())
        .andExpect(jsonPath("$.generation.inputTokens").value(10))
        .andExpect(jsonPath("$.generation.outputTokens").value(5))
        .andExpect(jsonPath("$.generationModel").value("test-standard"))
        .andExpect(jsonPath("$.content").value("A record carries data."));

    assertThat(PROVIDER.chatRequests()).hasSize(1);
    var sent = JSON.readTree(PROVIDER.chatRequests().getFirst());
    assertThat(sent.get("model").stringValue()).isEqualTo("test-standard");
    assertThat(sent.at("/messages/1/content").stringValue()).isEqualTo(prompt);
    assertThat(sent.get("max_completion_tokens").intValue()).isEqualTo(4096);
    assertThat(sent.has("temperature")).isFalse();
  }

  @ParameterizedTest
  @ValueSource(strings = {"/chat", "/route"})
  void rejectsOversizedPromptsWithoutProviderCalls(String path) throws Exception {
    String body = JSON.writeValueAsString(Map.of("prompt", "x".repeat(12001)));
    mvc.perform(post(path).contentType("application/json").content(body))
        .andExpect(status().isBadRequest());
    assertThat(PROVIDER.jevRequests()).isEmpty();
    assertThat(PROVIDER.chatRequests()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(ints = {401, 429, 503})
  void stopsBeforeOpenAiWhenJevFailsAndDoesNotLeakProviderDetails(int statusCode) throws Exception {
    PROVIDER.jev(statusCode, "{\"error\":\"provider-secret and private prompt\"}");
    var result = mvc.perform(post("/chat").contentType("application/json").content("{\"prompt\":\"private prompt\"}"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("AI_PROVIDER_UNAVAILABLE"))
        .andReturn();
    assertThat(result.getResponse().getContentAsString()).doesNotContain("provider-secret", "private prompt");
    assertThat(PROVIDER.jevRequests()).hasSize(1);
    assertThat(PROVIDER.chatRequests()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {
      "{\"model\":\"jev-1.13.0\",\"answers\":{}}",
      "{\"model\":\"jev-1.13.0\",\"answers\":{\"wrong_key\":{\"type\":\"noul\",\"noul\":0.9}}}",
      "{\"model\":\"jev-1.13.0\",\"answers\":{\"complexity\":{\"type\":\"noul\",\"noul\":0.9}}}",
      "{\"model\":\"jev-1.13.0\",\"answers\":{\"complexity\":{\"type\":\"choice\",\"choice\":\"UNKNOWN\",\"confidence\":0.9,\"probabilities\":{}}}}",
      "not-json"
  })
  void rejectsUnusableJevResponsesBeforeGeneratingAnAnswer(String response) throws Exception {
    PROVIDER.jev(200, response);
    mvc.perform(post("/chat").contentType("application/json").content("{\"prompt\":\"Hello\"}"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("AI_RESPONSE_INVALID"));
    assertThat(PROVIDER.chatRequests()).isEmpty();
  }

  @Test
  void reportsAnOpenAiFailureWithoutRetryingOrExposingItsMessage() throws Exception {
    PROVIDER.jev(200, choice("STANDARD", 0.9));
    PROVIDER.chat(429, "{\"error\":{\"message\":\"secret-upstream-detail\",\"type\":\"rate_limit_error\",\"code\":\"rate_limit_exceeded\"}}");
    var result = mvc.perform(post("/chat").contentType("application/json").content("{\"prompt\":\"Hello\"}"))
        .andExpect(status().isServiceUnavailable()).andReturn();
    assertThat(result.getResponse().getContentAsString()).doesNotContain("secret-upstream-detail");
    assertThat(PROVIDER.chatRequests()).hasSize(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "length", "content_filter"})
  void rejectsEmptyOrIncompleteChatCompletions(String finishReason) throws Exception {
    PROVIDER.jev(200, choice("ROUTINE", 0.9));
    PROVIDER.chat(200, """
        {"id":"chatcmpl-test","object":"chat.completion","created":1,"model":"test-routine",
         "choices":[{"index":0,"message":{"role":"assistant","content":"%s"},"finish_reason":"%s"}]}
        """.formatted(finishReason.isEmpty() ? "" : "Partial answer", finishReason.isEmpty() ? "stop" : finishReason));
    mvc.perform(post("/chat").contentType("application/json").content("{\"prompt\":\"Hello\"}"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("AI_RESPONSE_INVALID"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"{", "{\"prompt\":{\"private\":\"sensitive-text\"}}"})
  void returnsASafeProblemForMalformedRequests(String body) throws Exception {
    var result = mvc.perform(post("/chat").contentType("application/json").content(body))
        .andExpect(status().isBadRequest()).andReturn();
    assertThat(result.getResponse().getContentAsString()).doesNotContain("sensitive-text");
    assertThat(PROVIDER.jevRequests()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-json", "{}", "{\"id\":\"chatcmpl-test\",\"object\":\"chat.completion\",\"created\":1,\"model\":\"test-routine\",\"choices\":[]}"})
  void rejectsMalformedOrMissingOpenAiCompletions(String response) throws Exception {
    PROVIDER.jev(200, choice("ROUTINE", 0.9));
    PROVIDER.chat(200, response);
    mvc.perform(post("/chat").contentType("application/json").content("{\"prompt\":\"Hello\"}"))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.code").value("AI_RESPONSE_INVALID"));
    assertThat(PROVIDER.chatRequests()).hasSize(1);
  }

}
