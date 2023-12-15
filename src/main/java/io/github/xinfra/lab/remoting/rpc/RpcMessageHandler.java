package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.processor.RemotingProcessor;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
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

        RpcRequestMessageProcessor rpcRequestMessageProcessor = new RpcRequestMessageProcessor(rpcMessageFactory,
                defaultExecutor, userProcessors);

        remotingProcessors.put(request, rpcRequestMessageProcessor);
        remotingProcessors.put(MessageType.onewayRequest, rpcRequestMessageProcessor);

        remotingProcessors.put(MessageType.response, new RpcResponseMessageProcessor());

        RpcHeartbeatMessageProcessor rpcHeartbeatMessageProcessor = new RpcHeartbeatMessageProcessor(rpcMessageFactory);
        remotingProcessors.put(MessageType.heartbeatRequest, rpcHeartbeatMessageProcessor);
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
        String errorMsg = String.format("handle %s message fail, id: %s", messageType, rpcMessage.id());
        log.error(errorMsg);
        if (request == messageType || heartbeatRequest == messageType) {
            RpcResponseMessage responseMessage = rpcMessageFactory.createExceptionResponse(rpcMessage.id(), t, errorMsg);
            RpcResponses.sendResponse(remotingContext, responseMessage, rpcMessageFactory);
        }
    }

    @Override
    public void registerUserProcessor(UserProcessor<?> userProcessor) {

    }
}
