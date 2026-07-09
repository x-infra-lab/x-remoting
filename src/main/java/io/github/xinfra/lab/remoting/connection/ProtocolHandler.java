package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.message.Message;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@ChannelHandler.Sharable
public class ProtocolHandler extends ChannelDuplexHandler {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		if (msg instanceof Message) {
			Connection connection = ctx.channel().attr(CONNECTION).get();
			connection.getProtocol().getMessageHandler().handleMessage(ctx, (Message) msg);
		}
		else {
			ctx.fireChannelRead(msg);
		}
	}

}
