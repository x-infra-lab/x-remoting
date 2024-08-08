package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.message.Message;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@ChannelHandler.Sharable
public class ProtocolHandler extends ChannelDuplexHandler {

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof Message) {
			Connection connection = ctx.channel().attr(CONNECTION).get();
			connection.getProtocol().messageHandler().handleMessage(new MessageHandlerContext(ctx), msg);
		}
		else {
			ctx.fireChannelRead(msg);
		}
	}

}
