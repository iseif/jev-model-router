package dev.iseif.jevmodelrouter.model;

/** Task requirements, independent of provider model names and prices. */
public enum TaskComplexity {

  ROUTINE("""
      A direct transformation or short answer with almost no reasoning: greet someone,
      reformat supplied text, correct spelling, or answer a single basic factual question.
      No diagnosis, trade-off analysis, or interacting constraints are needed.
      """),

  STANDARD("""
      A familiar, self-contained task with a few straightforward steps: summarize supplied
      material, explain a well-known concept, draft a short message, or write one small
      function with clear requirements. No subtle edge cases or competing design goals.
      """),

  COMPLEX("""
      A bounded analysis requiring several connected reasoning steps using established
      methods: diagnose a bug from supplied evidence, trace interacting code paths and
      edge cases, compare specified alternatives, or synthesize supplied sources.
      Familiar synchronization problems with a known repair belong here.
      """),

  DEMANDING("""
      An open-ended problem requiring a new design or substantial original argument
      across many interacting constraints: derive and justify distributed-system
      invariants through failures and recovery, or construct a nontrivial proof without
      a supplied method. A technical topic or familiar bug pattern alone does not qualify.
      """);

  private final String description;

  TaskComplexity(String description) {
    this.description = description;
  }

  public String description() {
    return description;
  }
}
