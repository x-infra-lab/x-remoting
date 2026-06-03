package io.github.xinfra.lab.remoting.connection;

public class NoReconnectPolicy implements BackoffPolicy {

	@Override
	public long nextDelayNanos(int attempts) {
		return -1L;
	}

}
