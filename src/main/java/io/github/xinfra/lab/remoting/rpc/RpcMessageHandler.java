package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.processor.RemotingProcessor;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.github.xinfra.lab.remoting.message.MessageType.heartbeatRequest;
import static io.github.xinfra.lab.remoting.message.MessageType.request;

@Slf4j
public class RpcMessageHandler implements MessageHandler {

    private Map<MessageType, RemotingProcessor<RpcMessage>> remotingProcessors;

    private Map<String, UserProcessor<?>> userProcessors;

    private RpcMessageFactory rpcMessageFactory;

    // TODO: use config
    private Executor defaultExecutor = new ThreadPoolExecutor(20, 400, 60,
            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1024),
            new NamedThreadFactory("RPC-MESSAGE-HANDLER-")
    );

    public RpcMessageHandler(RpcMessageFactory rpcMessageFactory) {
        this.rpcMessageFactory = rpcMessageFactory;

        RpcRequestMessageProcessor rpcRequestMessageProcessor = new RpcRequestMessageProcessor(rpcMessageFactory, userProcessors);

        remotingProcessors.put(request, rpcRequestMessageProcessor);
        remotingProcessors.put(MessageType.onewayRequest, rpcRequestMessageProcessor);
        remotingProcessors.put(MessageType.response, new RpcResponseMessageProcessor());

        RpcHeartbeatMessageProcessor rpcHeartbeatMessageProcessor = new RpcHeartbeatMessageProcessor();
        remotingProcessors.put(MessageType.heartbeatRequest, rpcHeartbeatMessageProcessor);
        remotingProcessors.put(MessageType.heartbeatResponse, rpcHeartbeatMessageProcessor);
    }

    @Override
    public Executor executor() {
        return defaultExecutor;
    }

    @Override
    public void handleMessage(RemotingContext remotingContext, Object msg) {
        RpcMessage rpcMessage = (RpcMessage) msg;
        try {
            remotingProcessors.get(rpcMessage.messageType())
                    .handleMessage(remotingContext, rpcMessage);
        } catch (Throwable t) {
            exceptionForMessage(remotingContext, rpcMessage, t);
        }
    }

    private void exceptionForMessage(RemotingContext remotingContext, RpcMessage rpcMessage, Throwable t) {
        MessageType messageType = rpcMessage.messageType();
        log.error("Exception caught when handle {} message, id:{}", messageType, rpcMessage.id());
        if (request == messageType || heartbeatRequest == messageType) {

            RpcResponseMessage response = rpcMessageFactory.createExceptionResponse(rpcMessage.id(), ResponseStatus.SERVER_EXCEPTION, t);
            remotingContext.getChannelContext().writeAndFlush(response).addListener(
                    new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                if (log.isInfoEnabled()) {
                                    log.info("Write back exception response success, id={}, status={}",
                                            rpcMessage.id(), response.getStatus());
                                }
                            } else {
                                log.error("Write back exception response fail, id={}, status={}",
                                        rpcMessage.id(), response.getStatus(), future.cause());
                            }
                        }
                    }
            );

        }
    }

    @Override
    public void registerUserProcessor(UserProcessor<?> userProcessor) {

    }
}
