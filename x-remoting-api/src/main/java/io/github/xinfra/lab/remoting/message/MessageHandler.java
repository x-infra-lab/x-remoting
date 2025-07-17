package io.github.xinfra.lab.remoting.message;

import io.netty.channel.ChannelHandlerContext;

import java.io.Closeable;

public interface MessageHandler extends Closeable {

	void registerMessageTypeHandler(MessageTypeHandler messageTypeHandler);

	MessageTypeHandler messageTypeHandler(MessageType messageType);

	default void handleMessage(ChannelHandlerContext ctx, Message msg) {
		messageTypeHandler(msg.messageType()).handleMessage(ctx, msg);
	}

}
