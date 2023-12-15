package io.github.xinfra.lab.remoting.processor;

import java.util.concurrent.Executor;

public interface UserProcessor<T> {
    String interest();

    Object handRequest(T request);

    ExecutorSelector executorSelector();

    Executor executor();

    ClassLoader getBizClassLoader();


    interface ExecutorSelector {
        Executor select(String requestClass, Object requestHeader);
    }
}
