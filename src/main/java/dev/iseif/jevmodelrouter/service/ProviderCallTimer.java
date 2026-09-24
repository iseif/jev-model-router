package dev.iseif.jevmodelrouter.service;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** Times each blocking client operation, including failures, without logging request content. */
@Component
public class ProviderCallTimer {

  private static final Logger logger = LoggerFactory.getLogger(ProviderCallTimer.class);
  private final LongSupplier nanoTime;

  @Autowired
  public ProviderCallTimer() {
    this(System::nanoTime);
  }

  ProviderCallTimer(LongSupplier nanoTime) {
    this.nanoTime = nanoTime;
  }

  public <T> TimedResult<T> measure(ProviderOperation operation, String model, Supplier<T> call) {
    long started = nanoTime.getAsLong();
    boolean completed = false;
    T result;
    long durationMs;
    try {
      result = call.get();
      completed = true;
    } finally {
      durationMs = TimeUnit.NANOSECONDS.toMillis(nanoTime.getAsLong() - started);
      logger.info("AI call provider={}, operation={}, model={}, outcome={}, durationMs={}",
          operation.provider(), operation.label(), model, completed ? "completed" : "failed", durationMs);
    }
    return new TimedResult<>(result, durationMs);
  }

  public record TimedResult<T>(T value, long durationMs) {}
}
