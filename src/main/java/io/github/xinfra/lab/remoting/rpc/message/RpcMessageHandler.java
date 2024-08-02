package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.processor.MessageProcessor;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.RpcHeartbeatMessageProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.RpcRequestMessageProcessor;
import io.github.xinfra.lab.remoting.rpc.processor.RpcResponseMessageProcessor;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static io.github.xinfra.lab.remoting.message.MessageType.heartbeatRequest;
import static io.github.xinfra.lab.remoting.message.MessageType.request;

@Slf4j
public class RpcMessageHandler implements MessageHandler, Closeable {

    private Timer timer;

    private ConcurrentHashMap<MessageType, MessageProcessor<RpcMessage>> messageProcessors = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, UserProcessor<?>> userProcessors = new ConcurrentHashMap<>();

    // TODO: use config
    private ExecutorService executor = new ThreadPoolExecutor(20, 400, 60,
            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(1024),
            new NamedThreadFactory("Rpc-Message-Handler")
    );

    public RpcMessageHandler() {
        this.timer = new HashedWheelTimer(new NamedThreadFactory(getClass().getName() + "-Timer"));

        // request
        RpcRequestMessageProcessor rpcRequestMessageProcessor = new RpcRequestMessageProcessor();
        this.registerMessageProcessor(MessageType.request, rpcRequestMessageProcessor);
        this.registerMessageProcessor(MessageType.onewayRequest, rpcRequestMessageProcessor);
        // response
        this.registerMessageProcessor(MessageType.response, new RpcResponseMessageProcessor());
        // heartbeat
        this.registerMessageProcessor(MessageType.heartbeatRequest, new RpcHeartbeatMessageProcessor());
    }

    @Override
    public ExecutorService executor() {
        return executor;
    }

    @Override
    public void handleMessage(MessageHandlerContext messageHandlerContext, Object msg) {
        RpcMessage rpcMessage = (RpcMessage) msg;
        try {
            rpcMessage.setRemoteAddress(messageHandlerContext.getConnection().remoteAddress());
            messageProcessors.get(rpcMessage.messageType())
                    .handleMessage(messageHandlerContext, rpcMessage);
        } catch (Throwable t) {
            exceptionForMessage(messageHandlerContext, rpcMessage, t);
        }
    }

    @Override
    public void registerMessageProcessor(MessageType messageType, MessageProcessor<?> messageProcessor) {
        MessageProcessor<RpcMessage> prevMessageProcessor = messageProcessors.putIfAbsent(messageType, (MessageProcessor<RpcMessage>) messageProcessor);
        if (prevMessageProcessor != null) {
            throw new RuntimeException("repeat register message processor for " + messageType);
        }
    }

    @Override
    public MessageProcessor<RpcMessage> messageProcessor(MessageType messageType) {
        return messageProcessors.get(messageType);
    }

    @Override
    public void registerUserProcessor(UserProcessor<?> userProcessor) {
        UserProcessor<?> prevUserProcessor = userProcessors.putIfAbsent(userProcessor.interest(), userProcessor);
        if (prevUserProcessor != null) {
            throw new RuntimeException("repeat register user processor for " + userProcessor.interest());
        }
    }

    @Override
    public UserProcessor<?> userProcessor(String contentType) {
        return userProcessors.get(contentType);
    }

    @Override
    public Timer timer() {
        return timer;
    }

    private void exceptionForMessage(MessageHandlerContext messageHandlerContext, RpcMessage rpcMessage, Throwable t) {
        MessageType messageType = rpcMessage.messageType();
        String errorMsg = String.format("handle %s message fail, id: %s", messageType, rpcMessage.id());
        log.error(errorMsg);
        if (request == messageType || heartbeatRequest == messageType) {
            RpcResponseMessage responseMessage = messageHandlerContext
                    .getMessageFactory()
                    .createExceptionResponse(rpcMessage.id(), t, errorMsg);
            RpcResponses.sendResponse(messageHandlerContext, responseMessage);
        }
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
        timer.stop();
    }
}
