package io.github.xinfra.lab.remoting.connection;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;

@ChannelHandler.Sharable
public class ProtocolHeartBeatHandler extends ChannelInboundHandlerAdapter {

	Heartbeater heartbeater;

	public ProtocolHeartBeatHandler(Heartbeater heartbeater) {
		this.heartbeater = heartbeater;
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof IdleStateEvent) {
			Connection connection = ctx.channel().attr(Connection.CONNECTION).get();
			heartbeater.triggerHeartBeat(connection);
		}
		else {
			super.userEventTriggered(ctx, evt);
		}
	}

}
