package dev.iseif.jevmodelrouter.model;

public record RoutedChatResponse(RoutingDecision routingDecision, String content,
    String generationModel, CallMetrics generation, long totalDurationMs) {
}
