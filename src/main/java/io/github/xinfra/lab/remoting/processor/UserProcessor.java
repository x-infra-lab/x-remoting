package io.github.xinfra.lab.remoting.processor;

import java.util.concurrent.Executor;

public interface UserProcessor<T> {
    String interest();

    Object handRequest(T request);

    Executor select(String contentType , Object header);

    ClassLoader getBizClassLoader();
}
