package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.client.InvokeCallBack;
import io.github.xinfra.lab.remoting.message.Message;

public interface RpcInvokeCallBack<R> extends InvokeCallBack {
    @Override
    default void complete(Message message) {
        // TODO

    }


    void onException(Throwable t);

    void onResponse(R response);
}