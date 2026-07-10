package io.github.xinfra.lab.remoting.rpc.handler;

import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public abstract class BlockingRequestHandler<T, R> implements RequestHandler<T, R> {

	public abstract R handleRequest(T request);

	@Override
	public void handle(T request, ResponseObserver<R> responseObserver) {
		try {
			R result = handleRequest(request);
			responseObserver.complete(result);
		}
		catch (Exception e) {
			log.error("BlockingRequestHandler error: {}", e.getMessage(), e);
			responseObserver.onError(e);
		}
	}

	public static <T, R> BlockingRequestHandler<T, R> of(Function<T, R> fn) {
		return new BlockingRequestHandler<T, R>() {
			@Override
			public R handleRequest(T request) {
				return fn.apply(request);
			}
		};
	}

}
