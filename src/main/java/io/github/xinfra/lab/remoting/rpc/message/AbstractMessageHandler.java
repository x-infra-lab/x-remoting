package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@Slf4j
public abstract class AbstractMessageHandler implements MessageHandler {

	private ConcurrentHashMap<MessageType, MessageTypeHandler<? extends Message>> messageTypeHandlers = new ConcurrentHashMap<>();

	public AbstractMessageHandler() {
		this.registerMessageTypeHandler(new ResponseMessageTypeHandler());
		this.registerMessageTypeHandler(new HeartbeatRequestMessageTypeHandler());
	}

	public void registerMessageTypeHandler(MessageTypeHandler messageTypeHandler) {
		MessageTypeHandler prev = messageTypeHandlers.put(messageTypeHandler.getMessageType(), messageTypeHandler);
		if (prev != null) {
			log.warn("messageTypeHandler {} is overridden by {}", prev, messageTypeHandler);
		}
	}

	public MessageTypeHandler messageTypeHandler(MessageType messageType) {
		return messageTypeHandlers.get(messageType);
	}

	@Override
	public void handleMessage(ChannelHandlerContext ctx, io.github.xinfra.lab.remoting.message.Message transportMsg) {
		Message msg = (Message) transportMsg;
		Connection connection = ctx.channel().attr(CONNECTION).get();
		try {
			MessageTypeHandler messageTypeHandler = messageTypeHandler(msg.getMessageType());
			messageTypeHandler.handleMessage(connection, msg);
		}
		catch (Exception e) {
			log.error("MessageHandler handleMessage ex", e);
			Responses.handleExceptionResponse(connection, msg, e);
		}
	}

}
