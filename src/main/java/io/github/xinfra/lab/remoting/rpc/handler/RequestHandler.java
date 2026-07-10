package io.github.xinfra.lab.remoting.rpc.handler;

import java.util.concurrent.Executor;

public interface RequestHandler<T, R> {

	void handle(T request, ResponseObserver<R> responseObserver);

	default Executor getExecutor() {
		return null;
	}

}
