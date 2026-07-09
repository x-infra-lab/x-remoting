package io.github.xinfra.lab.remoting.connection;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class ExponentialBackoffWithJitter implements BackoffPolicy {

	private final long initialNanos;

	private final long maxNanos;

	private final double multiplier;

	private final double jitter;

	public ExponentialBackoffWithJitter(long initial, long max, TimeUnit unit, double multiplier, double jitter) {
		if (initial <= 0) {
			throw new IllegalArgumentException("initial must be > 0");
		}
		if (max < initial) {
			throw new IllegalArgumentException("max must be >= initial");
		}
		if (multiplier <= 1.0) {
			throw new IllegalArgumentException("multiplier must be > 1.0");
		}
		if (jitter < 0.0 || jitter > 1.0) {
			throw new IllegalArgumentException("jitter must be in [0, 1]");
		}
		this.initialNanos = unit.toNanos(initial);
		this.maxNanos = unit.toNanos(max);
		this.multiplier = multiplier;
		this.jitter = jitter;
	}

	public static ExponentialBackoffWithJitter defaults() {
		return new ExponentialBackoffWithJitter(1, 30, TimeUnit.SECONDS, 2.0, 0.5);
	}

	@Override
	public long nextDelayNanos(int attempts) {
		int safeAttempts = Math.max(0, attempts);
		double base = initialNanos * Math.pow(multiplier, safeAttempts);
		long capped = (long) Math.min(base, (double) maxNanos);
		if (jitter == 0.0) {
			return capped;
		}
		double factor = (1.0 - jitter) + (2.0 * jitter * ThreadLocalRandom.current().nextDouble());
		return Math.max(1L, (long) (capped * factor));
	}

}
