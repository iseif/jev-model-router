package dev.iseif.jevmodelrouter.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ModelRoutingProperties.class, AnswerReviewProperties.class})
public class AiConfiguration {

  @Bean
  ChatClient routedChatClient(ChatClient.Builder builder,
      @Value("classpath:prompts/chat-system.txt") Resource systemPrompt) {
    return builder.defaultSystem(systemPrompt).build();
  }
}
