package model;

import lombok.extern.log4j.Log4j2;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Log4j2
public record BackoffConfig(long initialDelay, double multiplier, long maxDelay, long jitterRange){

	public BackoffConfig(long initialDelay, double multiplier, long maxDelay) {
		this(initialDelay, multiplier, maxDelay, 0);
	}

	public long calculateBackoff(int retryAttempt) {
		long delay = (long) Math.pow(multiplier, retryAttempt);
		delay = TimeUnit.SECONDS.toMillis(delay);
		return Math.min(delay, maxDelay);
	}

	public long calculateRandomBackoff(int retryAttempt) {
		long delay = calculateBackoff(retryAttempt);
		long jitter = ThreadLocalRandom.current().nextLong(-jitterRange, jitterRange);
		delay = Math.min(maxDelay, delay + jitter);
		return Math.max(0, delay);
	}

	public static BackoffConfig withDefault() {
		return new BackoffConfig(1000L, 2, 60_000L, 500L);
	}
}

