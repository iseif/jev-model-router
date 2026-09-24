package dev.iseif.jevmodelrouter.service;

import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIInvalidDataException;
import dev.iseif.jevmodelrouter.model.CallMetrics;
import dev.iseif.jevmodelrouter.model.GeneratedAnswer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.EmptyUsage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;

@Component
public class OpenAiAnswerGenerator implements AnswerGenerator {

  private final ChatClient chatClient;
  private final ProviderCallTimer timer;

  public OpenAiAnswerGenerator(ChatClient chatClient, ProviderCallTimer timer) {
    this.chatClient = chatClient;
    this.timer = timer;
  }

  @Override
  public GeneratedAnswer generate(String prompt, String modelId) {
    var call = timer.measure(ProviderOperation.OPENAI_GENERATION, modelId, () -> {
      try {
        return chatClient.prompt()
            .messages(new UserMessage(prompt))
            .options(ChatOptions.builder().model(modelId))
            .call()
            .chatResponse();
      } catch (OpenAIInvalidDataException | ArithmeticException exception) {
        // Spring AI converts provider token counts from long to int with overflow checks.
        throw new AiUpstreamException(AiUpstreamException.Kind.INVALID_RESPONSE, exception);
      } catch (OpenAIException exception) {
        throw new AiUpstreamException(AiUpstreamException.Kind.UNAVAILABLE, exception);
      }
    });
    var response = call.value();
    if (response == null || response.getResult() == null) {
      throw AiUpstreamException.invalidResponse();
    }
    var result = response.getResult();
    String content = result.getOutput().getText();
    String finishReason = result.getMetadata().getFinishReason();
    if (content == null || content.isBlank()
        || "length".equalsIgnoreCase(finishReason) || "content_filter".equalsIgnoreCase(finishReason)) {
      throw AiUpstreamException.invalidResponse();
    }
    var usage = response.getMetadata().getUsage();
    return new GeneratedAnswer(content, response.getMetadata().getModel(),
        new CallMetrics(call.durationMs(),
            usage instanceof EmptyUsage ? null : usage.getPromptTokens(),
            usage instanceof EmptyUsage ? null : usage.getCompletionTokens()));
  }
}
