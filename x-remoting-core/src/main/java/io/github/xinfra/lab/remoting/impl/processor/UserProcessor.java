package io.github.xinfra.lab.remoting.impl.processor;

import io.github.xinfra.lab.remoting.impl.message.RemotingMessageHeader;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public interface UserProcessor<T> {

	String interest();

	Object handRequest(T request);

	default ExecutorService executor() {
		return null;
	}

	default ExecutorSelector executorSelector() {
		return null;
	}

	default ClassLoader getBizClassLoader() {
		return null;
	}

	interface ExecutorSelector {

		Executor select(String contentType, RemotingMessageHeader remotingMessageHeader);

	}

}
