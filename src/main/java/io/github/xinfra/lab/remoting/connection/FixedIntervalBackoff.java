package io.github.xinfra.lab.remoting.connection;

import java.util.concurrent.TimeUnit;

public class FixedIntervalBackoff implements BackoffPolicy {

	private final long intervalNanos;

	public FixedIntervalBackoff(long interval, TimeUnit unit) {
		if (interval <= 0) {
			throw new IllegalArgumentException("interval must be > 0");
		}
		this.intervalNanos = unit.toNanos(interval);
	}

	@Override
	public long nextDelayNanos(int attempts) {
		return intervalNanos;
	}

}
