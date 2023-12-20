package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.processor.RemotingProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executor;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@Slf4j
public class RpcResponseMessageProcessor implements RemotingProcessor<RpcMessage> {

    private Executor executor;

    public RpcResponseMessageProcessor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void handleMessage(RemotingContext remotingContext, RpcMessage message) {
        executor.execute(new ProcessTask(remotingContext, (RpcResponseMessage) message));
    }

    private void doProcess(RemotingContext remotingContext, RpcResponseMessage responseMessage) {
        int id = responseMessage.id();
        Connection connection = remotingContext.getChannelContext().channel().attr(CONNECTION).get();
        InvokeFuture future = connection.removeInvokeFuture(id);
        if (future != null) {
            future.cancelTimeout();
            future.finish(responseMessage);

            ClassLoader contextClassLoader = null;
            try {
                ClassLoader appClassLoader = future.getAppClassLoader();
                if (appClassLoader != null) {
                    contextClassLoader = Thread.currentThread().getContextClassLoader();
                    Thread.currentThread().setContextClassLoader(appClassLoader);
                }
                future.executeCallBack();
            } finally {
                if (contextClassLoader != null) {
                    Thread.currentThread().setContextClassLoader(contextClassLoader);
                }
            }
        } else {
            log.warn("can not find InvokeFuture maybe timeout. id:{} status:{} from:{}",
                    responseMessage.id(), responseMessage.getStatus(),
                    remotingContext.getChannelContext().channel().remoteAddress());
        }
    }

    class ProcessTask implements Runnable {
        private RemotingContext remotingContext;
        private RpcResponseMessage responseMessage;

        public ProcessTask(RemotingContext remotingContext, RpcResponseMessage responseMessage) {
            this.remotingContext = remotingContext;
            this.responseMessage = responseMessage;
        }

        @Override
        public void run() {
            try {
                doProcess(remotingContext, responseMessage);
            } catch (Throwable t) {
                log.error("process response fail. id:{}, status:{}", responseMessage.id(),
                        responseMessage.getStatus(), t);
            }
        }
    }


}
