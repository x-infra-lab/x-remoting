package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.message.Message;

import java.util.concurrent.Executor;

public interface InvokeCallBack {
    void complete(InvokeFuture invokeFuture);

    default Executor executor() {
        return null;
    }
}
