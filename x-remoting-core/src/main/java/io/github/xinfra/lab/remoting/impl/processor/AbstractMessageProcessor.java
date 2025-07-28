package io.github.xinfra.lab.remoting.impl.processor;

import io.github.xinfra.lab.remoting.message.Message;

import java.util.concurrent.ExecutorService;

public abstract class AbstractMessageProcessor<T extends Message> implements MessageProcessor<T> {

	private ExecutorService executor;

	@Override
	public void executor(ExecutorService executor) {
		this.executor = executor;
	}

	@Override
	public ExecutorService executor() {
		return executor;
	}

}
