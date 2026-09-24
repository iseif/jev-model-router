package dev.iseif.jevmodelrouter.model;

/** Client-observed elapsed time, including network and decoding; absent token counts stay null. */
public record CallMetrics(long durationMs, Integer inputTokens, Integer outputTokens) {}
