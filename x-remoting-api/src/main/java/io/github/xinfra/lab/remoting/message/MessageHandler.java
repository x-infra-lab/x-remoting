package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.netty.channel.ChannelHandlerContext;

import java.io.Closeable;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

public interface MessageHandler extends Closeable {

	void registerMessageTypeHandler(MessageTypeHandler messageTypeHandler);

	MessageTypeHandler messageTypeHandler(MessageType messageType);

	default void handleMessage(ChannelHandlerContext ctx, Message msg) {
		Connection connection = ctx.channel().attr(CONNECTION).get();
		try {
			messageTypeHandler(msg.messageType()).handleMessage(connection, msg);
		}
		catch (Exception e) {
			// todo log
			ResponseMessage response = connection.getProtocol()
				.messageFactory()
				.createResponse(msg.id(), ResponseStatus.Error, e);
			Responses.sendResponse(connection, response);
		}
	}

}
