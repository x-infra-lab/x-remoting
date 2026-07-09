package io.github.xinfra.lab.remoting.connection;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicInteger;

public class HeartbeatState {

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
