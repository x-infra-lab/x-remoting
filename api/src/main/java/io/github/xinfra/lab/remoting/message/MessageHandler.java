package io.github.xinfra.lab.remoting.message;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MessageHandler {

	Logger log = LoggerFactory.getLogger(MessageHandler.class);

	void registerMessageTypeHandler(MessageTypeHandler messageTypeHandler);

	MessageTypeHandler messageTypeHandler(MessageType messageType);

	void handleMessage(ChannelHandlerContext ctx, Message msg);

}
