package dev.iseif.jevmodelrouter.service;

/** Provider details stay in the cause; HTTP responses use fixed, application-owned messages. */
public class AiUpstreamException extends RuntimeException {

  public enum Kind { UNAVAILABLE, INVALID_RESPONSE }

  private final Kind kind;

  public AiUpstreamException(Kind kind, Throwable cause) {
    super(kind.name(), cause);
    this.kind = kind;
  }

  public Kind kind() {
    return kind;
  }

  public static AiUpstreamException invalidResponse() {
    return new AiUpstreamException(Kind.INVALID_RESPONSE, null);
  }
}
