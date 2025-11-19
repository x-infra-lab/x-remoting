package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.ResponseStatusRuntimeException;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@Slf4j
public abstract class AbstractMessageHandler implements MessageHandler {

    private ConcurrentHashMap<MessageType, MessageTypeHandler<? extends Message>> messageTypeHandlers = new ConcurrentHashMap<>();

    public AbstractMessageHandler() {
        // response
        this.registerMessageTypeHandler(new ResponseMessageTypeHandler());
        this.registerMessageTypeHandler(new HeartbeatRequestMessageTypeHandler());
    }

    @Override
    public void registerMessageTypeHandler(MessageTypeHandler messageTypeHandler) {
        MessageTypeHandler prev = messageTypeHandlers.put(messageTypeHandler.messageType(), messageTypeHandler);
        if (prev != null) {
            log.warn("messageTypeHandler {} is overridden by {}", prev, messageTypeHandler);
        }
    }

    @Override
    public MessageTypeHandler messageTypeHandler(MessageType messageType) {
        return messageTypeHandlers.get(messageType);
    }

    @Override
    public void handleMessage(ChannelHandlerContext ctx, Message msg) {
        Connection connection = ctx.channel().attr(CONNECTION).get();
        try {
            MessageTypeHandler messageTypeHandler = messageTypeHandler(msg.messageType());
            Executor executor = connection.getExecutor();

            Runnable task = () -> {
                try {
                    messageTypeHandler.handleMessage(connection, msg);
                } catch (Exception e) {
                    log.error("MessageTypeHandler handleMessage ex", e);
                    handleException(connection, msg, e);
                }
            };
            executor.execute(task);
        } catch (Exception e) {
            log.error("MessageHandler handleMessage ex", e);
            handleException(connection, msg, e);
        }
    }

    private void handleException(Connection connection, Message msg, Exception e) {
        if (isNeedResponse(msg)) {
            if (e instanceof ResponseStatusRuntimeException) {
                ResponseStatusRuntimeException statusException = (ResponseStatusRuntimeException) e;
                ResponseMessage response = connection.getProtocol()
                        .messageFactory()
                        .createResponse(msg.id(), msg.serializationType(), statusException.getResponseStatus());
                Responses.sendResponse(connection, response);
            } else {
                ResponseMessage response = connection.getProtocol()
                        .messageFactory()
                        .createResponse(msg.id(), msg.serializationType(), ResponseStatus.UnKnownError, e);
                Responses.sendResponse(connection, response);
            }
        }
    }

    private boolean isNeedResponse(Message msg) {
        return msg instanceof RequestMessage && !Requests.isOnewayRequest((RequestMessage) msg);
    }
}
