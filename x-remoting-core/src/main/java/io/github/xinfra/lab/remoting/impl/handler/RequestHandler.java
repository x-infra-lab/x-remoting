package io.github.xinfra.lab.remoting.impl.handler;

public interface RequestHandler<T, R> {

    R handle(T request);
    
}
