package io.github.xinfra.lab.remoting.impl.handler;

public interface UserHandler<T, R> {

    R handle(T request);
    
}
