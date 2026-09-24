package dev.iseif.jevmodelrouter.service;

/** Stable provider/operation pairs for call-timing logs. */
public enum ProviderOperation {
  JEV_CLASSIFICATION("typesafe", "classification"),
  JEV_ANSWER_REVIEW("typesafe", "answer_review"),
  OPENAI_GENERATION("openai", "generation");

  private final String provider;
  private final String label;

  ProviderOperation(String provider, String label) {
    this.provider = provider;
    this.label = label;
  }

  public String provider() {
    return provider;
  }

  public String label() {
    return label;
  }
}
