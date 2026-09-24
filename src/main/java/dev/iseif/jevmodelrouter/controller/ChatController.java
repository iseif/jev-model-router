package dev.iseif.jevmodelrouter.controller;

import dev.iseif.jevmodelrouter.model.PromptRequest;
import dev.iseif.jevmodelrouter.model.RoutedChatResponse;
import dev.iseif.jevmodelrouter.model.RoutingDecision;
import dev.iseif.jevmodelrouter.service.ModelRouter;
import dev.iseif.jevmodelrouter.service.RoutedChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

  private final RoutedChatService chatService;
  private final ModelRouter modelRouter;

  public ChatController(RoutedChatService chatService, ModelRouter modelRouter) {
    this.chatService = chatService;
    this.modelRouter = modelRouter;
  }

  @PostMapping("/chat")
  public RoutedChatResponse chat(@Valid @RequestBody PromptRequest request) {
    return chatService.chat(request.prompt());
  }

  @PostMapping("/route")
  public RoutingDecision route(@Valid @RequestBody PromptRequest request) {
    return modelRouter.route(request.prompt());
  }
}
