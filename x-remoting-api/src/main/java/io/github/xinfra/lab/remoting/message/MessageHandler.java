package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

public interface MessageHandler {

	Logger log = LoggerFactory.getLogger(MessageHandler.class);

	void registerMessageTypeHandler(MessageTypeHandler messageTypeHandler);

	MessageTypeHandler messageTypeHandler(MessageType messageType);

	default void handleMessage(ChannelHandlerContext ctx, Message msg) {
		Connection connection = ctx.channel().attr(CONNECTION).get();
		try {
			MessageTypeHandler messageTypeHandler = messageTypeHandler(msg.messageType());
			Executor executor = connection.getExecutor();

			Runnable task = () -> {
				try {
					messageTypeHandler.handleMessage(connection, msg);
				}
				catch (Exception e) {
					log.error("MessageTypeHandler handleMessage ex", e);
					ResponseMessage response = connection.getProtocol()
						.messageFactory()
						.createResponse(msg.id(), ResponseStatus.Error, e);
					// todo oneway response的处理
					Responses.sendResponse(connection, response);
				}
			};
			executor.execute(task);
		}
		catch (Exception e) {
			log.error("MessageHandler handleMessage ex", e);
			ResponseMessage response = connection.getProtocol()
				.messageFactory()
				.createResponse(msg.id(), ResponseStatus.Error, e);
			// todo oneway response的处理
			Responses.sendResponse(connection, response);
		}
	}

}
