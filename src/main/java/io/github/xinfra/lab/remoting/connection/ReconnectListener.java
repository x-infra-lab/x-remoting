package io.github.xinfra.lab.remoting.connection;

import java.net.InetSocketAddress;

public interface ReconnectListener {

	default void onScheduled(InetSocketAddress address, int attempts, long delayNanos) {
	}

	default void onSuccess(InetSocketAddress address, int attempts) {
	}

	default void onFailure(InetSocketAddress address, int attempts, Throwable cause) {
	}

	default void onAbandoned(InetSocketAddress address, int attempts, Throwable lastCause) {
	}

}
