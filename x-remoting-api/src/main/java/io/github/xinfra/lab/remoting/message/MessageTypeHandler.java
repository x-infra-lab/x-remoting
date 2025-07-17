package io.github.xinfra.lab.remoting.message;

import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.Executor;

public interface MessageTypeHandler {

	MessageType messageType();

	void handleMessage(ChannelHandlerContext ctx, Message msg);

	Executor executor();

}
