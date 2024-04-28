package io.github.xinfra.lab.remoting.client;


import java.util.concurrent.Executor;

public interface InvokeCallBack {
    void complete(InvokeFuture future);

    default Executor executor() {
        return null;
    }
}
