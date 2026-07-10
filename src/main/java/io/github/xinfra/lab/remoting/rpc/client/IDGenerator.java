package io.github.xinfra.lab.remoting.rpc.client;

import java.util.concurrent.atomic.AtomicInteger;

public class IDGenerator {

	private final AtomicInteger counter = new AtomicInteger(0);

	public int nextRequestId() {
		return counter.getAndIncrement();
	}

}
