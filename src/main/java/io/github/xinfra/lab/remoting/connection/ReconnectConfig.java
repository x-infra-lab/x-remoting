package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.Validate;
import lombok.Getter;

/**
 * Immutable configuration for {@link DefaultReconnector}. Construct via
 * {@link #builder()}.
 *
 * <pre>
 * ReconnectConfig cfg = ReconnectConfig.builder()
 *         .backoffPolicy(ExponentialBackoffWithJitter.defaults())
 *         .maxAttempts(20)
 *         .workerThreads(8)
 *         .build();
 * </pre>
 */
@Getter
public final class ReconnectConfig {

	private final BackoffPolicy backoffPolicy;

	/** Max attempts before giving up; {@code <= 0} means unlimited. */
	private final int maxAttempts;

	/**
	 * Max total reconnect time before giving up (nanoseconds); {@code <= 0} means
	 * unlimited.
	 */
	private final long maxTotalDurationNanos;

	/** Worker threads used to run blocking connect attempts. */
	private final int workerThreads;

	private ReconnectConfig(Builder b) {
		Validate.notNull(b.backoffPolicy, "backoffPolicy must not be null");
		Validate.isTrue(b.workerThreads > 0, "workerThreads must be > 0, got %s", b.workerThreads);
		this.backoffPolicy = b.backoffPolicy;
		this.maxAttempts = b.maxAttempts;
		this.maxTotalDurationNanos = b.maxTotalDurationNanos;
		this.workerThreads = b.workerThreads;
	}

	/** Returns a builder pre-loaded with the default values. */
	public static Builder builder() {
		return new Builder();
	}

	/** Returns the default configuration. */
	public static ReconnectConfig defaults() {
		return builder().build();
	}

	public static final class Builder {

		private BackoffPolicy backoffPolicy = ExponentialBackoffWithJitter.defaults();

		private int maxAttempts = -1;

		private long maxTotalDurationNanos = -1L;

		private int workerThreads = 4;

		private Builder() {
		}

		public Builder backoffPolicy(BackoffPolicy backoffPolicy) {
			this.backoffPolicy = backoffPolicy;
			return this;
		}

		public Builder maxAttempts(int maxAttempts) {
			this.maxAttempts = maxAttempts;
			return this;
		}

		public Builder maxTotalDurationNanos(long maxTotalDurationNanos) {
			this.maxTotalDurationNanos = maxTotalDurationNanos;
			return this;
		}

		public Builder workerThreads(int workerThreads) {
			this.workerThreads = workerThreads;
			return this;
		}

		public ReconnectConfig build() {
			return new ReconnectConfig(this);
		}

	}

}
