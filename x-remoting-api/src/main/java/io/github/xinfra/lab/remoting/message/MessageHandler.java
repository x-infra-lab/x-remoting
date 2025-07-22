package io.github.xinfra.lab.remoting.message;

import io.netty.channel.ChannelHandlerContext;



public interface MessageHandler {

	void registerMessageTypeHandler(MessageTypeHandler messageTypeHandler);

	MessageTypeHandler messageTypeHandler(MessageType messageType);

	void handleMessage(ChannelHandlerContext ctx, Message msg) ;

}
