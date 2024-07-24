package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.client.InvokeCallBack;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponses;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;


public interface RpcInvokeCallBack<R> extends InvokeCallBack {
    Logger LOGGER = LoggerFactory.getLogger(RpcInvokeCallBack.class);

    @Override
    default void complete(Message message) {
        Runnable task = () -> {
            try {
                RpcResponseMessage responseMessage = (RpcResponseMessage) message;
                Object responseObject = RpcResponses.getResponseObject(responseMessage);
                try {
                    onResponse((R) responseObject);
                } catch (Throwable t) {
                    LOGGER.error("call back execute onResponse fail.", t);
                }
            } catch (Throwable t) {
                try {
                    onException(t);
                } catch (Throwable throwable) {
                    LOGGER.error("call back execute onException fail.", throwable);
                }
            }
        };

        Executor executor = this.executor();

        if (executor != null) {
            try {
                executor.execute(task);
            } catch (RejectedExecutionException re) {
                LOGGER.error("fail execute callback. id:{}", message.id(), re);
            }
        } else {
            task.run();
        }
    }

    void onException(Throwable t);

    void onResponse(R response);
}