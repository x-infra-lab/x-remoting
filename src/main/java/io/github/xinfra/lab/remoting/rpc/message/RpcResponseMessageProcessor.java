package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.connection.Connection;
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
            future.complete(responseMessage);
            try {
                future.executeCallBack();
            } catch (Throwable t) {
                log.error("executeCallBack fail. id:{}", responseMessage.id(), t);
                throw t;
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
                log.error("process response fail. id:{}, status:{} from:{}",
                        responseMessage.id(),
                        responseMessage.getStatus(),
                        remotingContext.getChannelContext().channel().remoteAddress()
                        , t);
            }
        }
    }


}
