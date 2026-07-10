package io.github.xinfra.lab.remoting.server;

import org.apache.commons.lang3.Validate;
import io.netty.util.Timer;
import lombok.Getter;

import java.util.concurrent.Executor;

@Getter
public final class ServerConfig {

	private final String hostName;

	private final int port;

	private final boolean manageConnection;

	private final boolean idleSwitch;

	private final long idleReaderTimeout;

	private final long idleWriterTimeout;

	private final long idleAllTimeout;

	private final Executor executor;

	private final Timer timer;

	private ServerConfig(Builder b) {
		Validate.inclusiveBetween(0, 0xFFFF, b.port, "port out of range: " + b.port);
		if (b.idleSwitch) {
			Validate.isTrue(b.idleReaderTimeout >= 0, "idleReaderTimeout must be >= 0, got %s", b.idleReaderTimeout);
			Validate.isTrue(b.idleWriterTimeout >= 0, "idleWriterTimeout must be >= 0, got %s", b.idleWriterTimeout);
			Validate.isTrue(b.idleAllTimeout >= 0, "idleAllTimeout must be >= 0, got %s", b.idleAllTimeout);
			Validate.isTrue(b.idleReaderTimeout > 0 || b.idleWriterTimeout > 0 || b.idleAllTimeout > 0,
					"at least one idle timeout must be > 0 when idleSwitch is true");
		}
		this.hostName = b.hostName;
		this.port = b.port;
		this.manageConnection = b.manageConnection;
		this.idleSwitch = b.idleSwitch;
		this.idleReaderTimeout = b.idleReaderTimeout;
		this.idleWriterTimeout = b.idleWriterTimeout;
		this.idleAllTimeout = b.idleAllTimeout;
		this.executor = b.executor;
		this.timer = b.timer;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static ServerConfig defaults() {
		return builder().build();
	}

	public static final class Builder {

		private String hostName;

		private int port = 0;

		private boolean manageConnection = false;

		private boolean idleSwitch = true;

		private long idleReaderTimeout = 0L;

		private long idleWriterTimeout = 0L;

		private long idleAllTimeout = 90000L;

		private Executor executor;

		private Timer timer;

		private Builder() {
		}

		public Builder hostName(String hostName) {
			this.hostName = hostName;
			return this;
		}

		public Builder port(int port) {
			this.port = port;
			return this;
		}

		public Builder manageConnection(boolean manageConnection) {
			this.manageConnection = manageConnection;
			return this;
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

		public Builder executor(Executor executor) {
			this.executor = executor;
			return this;
		}

		public Builder timer(Timer timer) {
			this.timer = timer;
			return this;
		}

		public ServerConfig build() {
			return new ServerConfig(this);
		}

	}

}
