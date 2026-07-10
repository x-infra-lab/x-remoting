package io.github.xinfra.lab.remoting.connection;

import org.apache.commons.lang3.Validate;
import io.netty.util.Timer;
import lombok.Getter;

import java.util.concurrent.ExecutorService;

/**
 * Immutable configuration for {@link DefaultConnectionFactory}. Construct via
 * {@link #builder()}.
 */
@Getter
public final class ConnectionFactoryConfig {

	private final boolean idleSwitch;

	private final long idleReaderTimeout;

	private final long idleWriterTimeout;

	private final long idleAllTimeout;

	private final int connectTimeout;

	/**
	 * Install Netty's {@code FlushConsolidationHandler} on each outbound channel. Trades
	 * a small latency increase for fewer write syscalls and higher throughput on
	 * batched/streaming workloads. Off by default — low-latency RPC usually wants the
	 * default behaviour.
	 */
	private final boolean useFlushConsolidation;

	/**
	 * Optional shared executor for {@link Connection} callbacks. Lifecycle is managed by
	 * the caller — x-remoting does not shut it down.
	 */
	private final ExecutorService executor;

	/**
	 * Optional shared timer for request timeouts. Lifecycle is managed by the caller —
	 * x-remoting does not shut it down.
	 */
	private final Timer timer;

	private ConnectionFactoryConfig(Builder b) {
		Validate.isTrue(b.connectTimeout > 0, "connectTimeout must be > 0, got %s", b.connectTimeout);
		if (b.idleSwitch) {
			Validate.isTrue(b.idleReaderTimeout >= 0, "idleReaderTimeout must be >= 0, got %s", b.idleReaderTimeout);
			Validate.isTrue(b.idleWriterTimeout >= 0, "idleWriterTimeout must be >= 0, got %s", b.idleWriterTimeout);
			Validate.isTrue(b.idleAllTimeout >= 0, "idleAllTimeout must be >= 0, got %s", b.idleAllTimeout);
			Validate.isTrue(b.idleReaderTimeout > 0 || b.idleWriterTimeout > 0 || b.idleAllTimeout > 0,
					"at least one of idleReaderTimeout / idleWriterTimeout / idleAllTimeout must be > 0 when idleSwitch is true");
		}
		this.idleSwitch = b.idleSwitch;
		this.idleReaderTimeout = b.idleReaderTimeout;
		this.idleWriterTimeout = b.idleWriterTimeout;
		this.idleAllTimeout = b.idleAllTimeout;
		this.connectTimeout = b.connectTimeout;
		this.useFlushConsolidation = b.useFlushConsolidation;
		this.executor = b.executor;
		this.timer = b.timer;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static ConnectionFactoryConfig defaults() {
		return builder().build();
	}

	public static final class Builder {

		private boolean idleSwitch = true;

		private long idleReaderTimeout = 15000L;

		private long idleWriterTimeout = 15000L;

		private long idleAllTimeout = 15000L;

		private int connectTimeout = 1000;

		private boolean useFlushConsolidation = false;

		private ExecutorService executor;

		private Timer timer;

		private Builder() {
		}

		public Builder idleSwitch(boolean idleSwitch) {
			this.idleSwitch = idleSwitch;
			return this;
		}

		public Builder idleReaderTimeout(long idleReaderTimeout) {
			this.idleReaderTimeout = idleReaderTimeout;
			return this;
		}

		public Builder idleWriterTimeout(long idleWriterTimeout) {
			this.idleWriterTimeout = idleWriterTimeout;
			return this;
		}

		public Builder idleAllTimeout(long idleAllTimeout) {
			this.idleAllTimeout = idleAllTimeout;
			return this;
		}

		public Builder connectTimeout(int connectTimeout) {
			this.connectTimeout = connectTimeout;
			return this;
		}

		public Builder useFlushConsolidation(boolean useFlushConsolidation) {
			this.useFlushConsolidation = useFlushConsolidation;
			return this;
		}

		public Builder executor(ExecutorService executor) {
			this.executor = executor;
			return this;
		}

		public Builder timer(Timer timer) {
			this.timer = timer;
			return this;
		}

		public ConnectionFactoryConfig build() {
			return new ConnectionFactoryConfig(this);
		}

	}

}
