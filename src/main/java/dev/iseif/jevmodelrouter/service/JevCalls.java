package dev.iseif.jevmodelrouter.service;

import java.util.function.Supplier;
import org.springaicommunity.typesafe.exception.TypeSafeAnswerTypeException;
import org.springaicommunity.typesafe.exception.TypeSafeApiConnectionException;
import org.springaicommunity.typesafe.exception.TypeSafeApiResponseValidationException;
import org.springaicommunity.typesafe.exception.TypeSafeException;
import org.springaicommunity.typesafe.exception.TypeSafeMissingAnswerException;
import org.springframework.web.client.RestClientException;

/** Translate the SDK's transport and decoding exceptions at the provider boundary. */
final class JevCalls {

  private JevCalls() {}

  static <T> T call(Supplier<T> request) {
    try {
      return request.get();
    } catch (TypeSafeApiResponseValidationException | TypeSafeMissingAnswerException | TypeSafeAnswerTypeException exception) {
      throw new AiUpstreamException(AiUpstreamException.Kind.INVALID_RESPONSE, exception);
    } catch (TypeSafeApiConnectionException exception) {
      // Connection exceptions retain a ResourceAccessException (a RestClientException) as cause.
      throw new AiUpstreamException(AiUpstreamException.Kind.UNAVAILABLE, exception);
    } catch (TypeSafeException exception) {
      // SDK 0.1.0 wraps JSON decoding failures in the base exception type.
      var kind = exception.getCause() instanceof RestClientException
          ? AiUpstreamException.Kind.INVALID_RESPONSE : AiUpstreamException.Kind.UNAVAILABLE;
      throw new AiUpstreamException(kind, exception);
    }
  }
}
