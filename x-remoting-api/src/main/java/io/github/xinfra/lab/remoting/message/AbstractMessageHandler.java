package io.github.xinfra.lab.remoting.message;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractMessageHandler implements MessageHandler {

    private ConcurrentHashMap<MessageType, MessageTypeHandler<RemotingMessage>>
            messageTypeHandlers = new ConcurrentHashMap<>();

    public AbstractMessageHandler() {
        // heartbeat
        this.registerMessageTypeHandler(new HeartbeatMessageTypeHandler());
        // response
        this.registerMessageTypeHandler(new ResponseMeesageTypeHandler());
    }

    @Override
    public void registerMessageTypeHandler(MessageTypeHandler messageTypeHandler) {

    }

    @Override
    public MessageTypeHandler messageTypeHandler(MessageType messageType) {
        return null;
    }

    @Override
    public void handleMessage(ChannelHandlerContext ctx, Message msg) {
        MessageHandler.super.handleMessage(ctx, msg);
    }
}
