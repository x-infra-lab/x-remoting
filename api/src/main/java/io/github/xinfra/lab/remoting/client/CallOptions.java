package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.common.Validate;
import io.github.xinfra.lab.remoting.message.DefaultMessageHeaders;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.Getter;

@Getter
public final class CallOptions {

	private final int timeoutMills;

	private final SerializationType serializationType;

	private final DefaultMessageHeaders headers;

	private CallOptions(Builder b) {
		Validate.isTrue(b.timeoutMills > 0, "timeoutMills must be > 0, got %s", b.timeoutMills);
		this.timeoutMills = b.timeoutMills;
		this.serializationType = b.serializationType;
		this.headers = b.headers;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static CallOptions defaults() {
		return builder().build();
	}

	public static final class Builder {

		private int timeoutMills = 3000;

		private SerializationType serializationType = SerializationType.Hession;

		private DefaultMessageHeaders headers = new DefaultMessageHeaders();

		private Builder() {
		}

		public Builder timeoutMills(int timeoutMills) {
			this.timeoutMills = timeoutMills;
			return this;
		}

		public Builder serializationType(SerializationType serializationType) {
			this.serializationType = serializationType;
			return this;
		}

		public Builder headers(DefaultMessageHeaders headers) {
			this.headers = headers;
			return this;
		}

		public CallOptions build() {
			return new CallOptions(this);
		}

	}

}
