package dev.iseif.jevmodelrouter.service;

import org.junit.jupiter.api.Test;
import org.springaicommunity.typesafe.exception.TypeSafeApiConnectionException;
import org.springaicommunity.typesafe.exception.TypeSafeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JevCallsTests {

  @Test
  void treatsAConnectionFailureAsUnavailableDespiteItsRestClientCause() {
    var failure = new TypeSafeApiConnectionException("timed out", new ResourceAccessException("socket"));
    assertThatThrownBy(() -> JevCalls.call(() -> { throw failure; }))
        .isInstanceOfSatisfying(AiUpstreamException.class,
            exception -> assertThat(exception.kind()).isEqualTo(AiUpstreamException.Kind.UNAVAILABLE))
        .hasCause(failure);
  }

  @Test
  void treatsSdkWrappedDecodingFailuresAsInvalidResponses() {
    var failure = new TypeSafeException("decoding failed", new RestClientException("malformed JSON"));
    assertThatThrownBy(() -> JevCalls.call(() -> { throw failure; }))
        .isInstanceOfSatisfying(AiUpstreamException.class,
            exception -> assertThat(exception.kind()).isEqualTo(AiUpstreamException.Kind.INVALID_RESPONSE));
  }
}
