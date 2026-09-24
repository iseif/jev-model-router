package dev.iseif.jevmodelrouter.controller;

import dev.iseif.jevmodelrouter.service.AiUpstreamException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(AiUpstreamException.class)
  ProblemDetail upstreamFailure(AiUpstreamException exception) {
    boolean invalid = exception.kind() == AiUpstreamException.Kind.INVALID_RESPONSE;
    var problem = ProblemDetail.forStatusAndDetail(
        invalid ? HttpStatus.BAD_GATEWAY : HttpStatus.SERVICE_UNAVAILABLE,
        invalid ? "An AI provider returned an unusable response." : "An AI provider could not complete the request.");
    problem.setProperty("code", invalid ? "AI_RESPONSE_INVALID" : "AI_PROVIDER_UNAVAILABLE");
    return problem;
  }
}
