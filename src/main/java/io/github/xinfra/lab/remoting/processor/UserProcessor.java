package io.github.xinfra.lab.remoting.processor;

import io.github.xinfra.lab.remoting.rpc.message.RpcMessageHeader;

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

		Executor select(String contentType, RpcMessageHeader rpcMessageHeader);

	}

}
