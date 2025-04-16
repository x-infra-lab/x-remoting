package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.rpc.processor.UserProcessor;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;

import java.util.concurrent.ExecutorService;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

public class MessageHandlerContext {

	@Getter
	private ChannelHandlerContext channelContext;

	@Getter
	private Connection connection;

	@Getter
	private Protocol protocol;

	@Getter
	private MessageFactory messageFactory;

	@Getter
	private ExecutorService messageDefaultExecutor;

	public MessageHandlerContext(ChannelHandlerContext ctx, RpcMessageHandler messageHandler) {
		this.channelContext = ctx;
		this.connection = ctx.channel().attr(CONNECTION).get();
		this.protocol = connection.getProtocol();
		this.messageFactory = protocol.messageFactory();
		this.messageDefaultExecutor = protocol.messageHandler().executor();
	}

	public UserProcessor<?> getUserProcessor(String contentType) {
		return messageHandler.userProcessor(contentType);
	}

}
