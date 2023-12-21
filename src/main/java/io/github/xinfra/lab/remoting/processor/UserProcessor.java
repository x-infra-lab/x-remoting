package io.github.xinfra.lab.remoting.processor;

import java.util.concurrent.Executor;

public interface UserProcessor<T> {
    String interest();

    Object handRequest(T request);

    default ExecutorSelector executorSelector() {
        return null;
    }

    default Executor executor() {
        return null;
    }

    default ClassLoader getBizClassLoader() {
        return null;
    }


    interface ExecutorSelector {
        Executor select(String requestClass, Object requestHeader);
    }
}
