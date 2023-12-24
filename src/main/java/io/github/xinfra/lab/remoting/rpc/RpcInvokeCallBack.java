package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.client.InvokeCallBack;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;


public interface RpcInvokeCallBack<R> extends InvokeCallBack {
    Logger LOGGER = LoggerFactory.getLogger(RpcInvokeCallBack.class);

    @Override
    default void complete(InvokeFuture future) {
        Runnable task = () -> {
            RpcResponseMessage responseMessage = null;
            SocketAddress remoteAddress = future.getConnection().getChannel().remoteAddress();

            try {

                try {
                    responseMessage = (RpcResponseMessage) future.await();
                } catch (Throwable t) {
                    String msg = "fail get response from InvokeFuture. remote address:" + remoteAddress;
                    LOGGER.error(msg, t);
                    throw new RemotingException(msg, t);
                }

                Object responseObject = RpcResponses.getResponseObject(responseMessage, remoteAddress);

                ClassLoader contextClassLoader = null;
                try {
                    if (future.getAppClassLoader() != null) {
                        contextClassLoader = Thread.currentThread().getContextClassLoader();
                        Thread.currentThread().setContextClassLoader(future.getAppClassLoader());
                    }
                    onResponse((R) responseObject);
                } catch (Throwable t) {
                    LOGGER.error("call back execute onResponse fail.", t);
                } finally {
                    if (contextClassLoader != null) {
                        Thread.currentThread().setContextClassLoader(contextClassLoader);
                    }
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
                LOGGER.error("fail execute callback. id:{}", future.getRequestId(), re);
            }
        } else {
            task.run();
        }
    }

    void onException(Throwable t);

    void onResponse(R response);
}