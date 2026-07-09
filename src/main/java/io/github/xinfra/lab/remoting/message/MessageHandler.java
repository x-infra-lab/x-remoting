package io.github.xinfra.lab.remoting.message;

import io.netty.channel.ChannelHandlerContext;

/**
 * SPI for handling decoded messages. Protocol implementations provide concrete handlers
 * that know how to dispatch their own message types.
 */
public interface MessageHandler {

	void handleMessage(ChannelHandlerContext ctx, Message msg);

}
