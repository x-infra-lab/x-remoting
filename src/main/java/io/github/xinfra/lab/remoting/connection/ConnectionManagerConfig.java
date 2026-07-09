package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.Validate;
import lombok.Getter;

/**
 * Immutable configuration for {@link AbstractConnectionManager}. Construct via
 * {@link #builder()}.
 */
@Getter
public final class ConnectionManagerConfig {

	/** Pool size per remote address. */
	private final int connectionNumPerEndpoint;

	private ConnectionManagerConfig(Builder b) {
		Validate.isTrue(b.connectionNumPerEndpoint > 0, "connectionNumPerEndpoint must be > 0, got %s",
				b.connectionNumPerEndpoint);
		this.connectionNumPerEndpoint = b.connectionNumPerEndpoint;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static ConnectionManagerConfig defaults() {
		return builder().build();
	}

	public static final class Builder {

		private int connectionNumPerEndpoint = 1;

		private Builder() {
		}

		public Builder connectionNumPerEndpoint(int connectionNumPerEndpoint) {
			this.connectionNumPerEndpoint = connectionNumPerEndpoint;
			return this;
		}

		public ConnectionManagerConfig build() {
			return new ConnectionManagerConfig(this);
		}

	}

}
