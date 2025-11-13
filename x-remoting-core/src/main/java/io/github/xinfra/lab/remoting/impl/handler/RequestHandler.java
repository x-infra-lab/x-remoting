package io.github.xinfra.lab.remoting.impl.handler;

public interface RequestHandler<T, R> {

    R handle(T request);

    default void asyncHandle(T request, ResponseObserver<R> responseObserver){
        R result = handle(request);
        responseObserver.complete(result);
    }

}
