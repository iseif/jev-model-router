package dev.iseif.jevmodelrouter.service;

import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class ProviderCallTimerTests {

  @Test
  void reportsMonotonicElapsedMillisecondsWithoutLoggingTheResult(CapturedOutput output) {
    var clock = new AtomicLong(1_000_000L);
    var timer = new ProviderCallTimer(clock::get);
    var result = timer.measure(ProviderOperation.JEV_CLASSIFICATION, "jev-test", () -> {
      clock.addAndGet(123_456_789L);
      return "private-response";
    });
    assertThat(result.durationMs()).isEqualTo(123);
    assertThat(result.value()).isEqualTo("private-response");
    assertThat(output.getOut()).contains("provider=typesafe", "operation=classification", "durationMs=123", "outcome=completed")
        .doesNotContain("private-response");
  }

  @Test
  void logsFailedCallsAndPropagatesTheOriginalFailure(CapturedOutput output) {
    var clock = new AtomicLong();
    var timer = new ProviderCallTimer(clock::get);
    var failure = new IllegalStateException("secret-provider-detail");
    assertThatThrownBy(() -> timer.measure(ProviderOperation.OPENAI_GENERATION, "test-model", () -> {
      clock.addAndGet(5_000_000_000L);
      throw failure;
    })).isSameAs(failure);
    assertThat(output.getOut()).contains("provider=openai", "operation=generation", "durationMs=5000", "outcome=failed")
        .doesNotContain("secret-provider-detail");
  }
}
