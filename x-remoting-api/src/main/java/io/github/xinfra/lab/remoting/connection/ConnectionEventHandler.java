package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.Validate;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@ChannelHandler.Sharable
@Slf4j
public class ConnectionEventHandler extends ChannelDuplexHandler {

	private ConnectionManager connectionManager;

	private ConnectionEventProcessor connectionEventProcessor;

	public ConnectionEventHandler(ConnectionManager connectionManager) {
		Validate.notNull(connectionManager, "connectionManager can not be null.");
		Validate.notNull(connectionManager.connectionEventProcessor(), "connectionEventProcessor can not be null.");
		this.connectionManager = connectionManager;
		this.connectionEventProcessor = connectionManager.connectionEventProcessor();
	}

	public ConnectionEventHandler(ConnectionEventProcessor connectionEventProcessor) {
		this.connectionEventProcessor = connectionEventProcessor;
	}

	@Override
	public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
		super.close(ctx, promise);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		// Q: why we do not trigger ConnectionEvent.CONNECT here?
		// A: At this point, the channel may not have a CONNECTION attribute
		// userEventTriggered(ctx, ConnectionEvent.CONNECT);

		super.channelActive(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Connection connection = ctx.channel().attr(CONNECTION).get();
		if (connectionManager != null && connectionManager.isStarted()) {
			connectionManager.close(connection);
		}
		userEventTriggered(ctx, ConnectionEvent.CLOSE);

		super.channelInactive(ctx);
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		if (evt instanceof ConnectionEvent) {
			Connection connection = ctx.channel().attr(CONNECTION).get();
			if (connectionEventProcessor.isStarted()) {
				connectionEventProcessor.handleEvent((ConnectionEvent) evt, connection);
			}
		}
		else {
			super.userEventTriggered(ctx, evt);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Channel channel = ctx.channel();
		SocketAddress localAddress = channel.localAddress();
		SocketAddress remoteAddress = channel.remoteAddress();
		log.warn("exceptionCaught channel localAddress:{} remoteAddress:{}, close the channel! cause by", localAddress,
				remoteAddress, cause);

		Connection connection = ctx.channel().attr(CONNECTION).get();
		connection.close();
	}

}
