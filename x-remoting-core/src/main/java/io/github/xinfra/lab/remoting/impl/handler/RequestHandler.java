package io.github.xinfra.lab.remoting.impl.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RequestHandler<T, R> {
    Logger log = LoggerFactory.getLogger(RequestHandler.class);

    R handle(T request);

    default void asyncHandle(T request, ResponseObserver<R> responseObserver) {
        try {
            R result = handle(request);
            responseObserver.complete(result);
        } catch (Exception e) {
            log.error("asyncHandle error", e);
            responseObserver.onError(e);
        }

    }

}
