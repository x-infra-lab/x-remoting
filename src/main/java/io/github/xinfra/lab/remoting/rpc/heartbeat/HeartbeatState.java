package io.github.xinfra.lab.remoting.rpc.heartbeat;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.netty.util.AttributeKey;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

public class HeartbeatState {

	public static final AttributeKey<HeartbeatState> KEY = AttributeKey.valueOf("rpc.heartbeatState");

	public static HeartbeatState of(Connection connection) {
		return connection.getChannel().attr(KEY).get();
	}

	public static HeartbeatState getOrCreate(Connection connection) {
		HeartbeatState existing = connection.getChannel().attr(KEY).get();
		if (existing != null) {
			return existing;
		}
		HeartbeatState state = new HeartbeatState();
		connection.getChannel().attr(KEY).set(state);
		return state;
	}

	private final AtomicInteger failCount = new AtomicInteger(0);

	@Getter
	@Setter
	private int timeoutMills = 3000;

	@Getter
	@Setter
	private int maxFailCount = 3;

	public int getFailCount() {
		return failCount.get();
	}

	public int incrementFailCount() {
		return failCount.incrementAndGet();
	}

	public void resetFailCount() {
		failCount.set(0);
	}

}
