package io.github.xinfra.lab.remoting.message;

import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractMessageHandler implements MessageHandler {

	private ConcurrentHashMap<MessageType, MessageTypeHandler<? extends Message>> messageTypeHandlers = new ConcurrentHashMap<>();

	public AbstractMessageHandler() {
		// heartbeat
		this.registerMessageTypeHandler(new HeartbeatMessageTypeHandler());
		// response
		this.registerMessageTypeHandler(new ResponseMeesageTypeHandler());
	}

	@Override
	public void registerMessageTypeHandler(MessageTypeHandler messageTypeHandler) {

	}

	@Override
	public MessageTypeHandler messageTypeHandler(MessageType messageType) {
		return null;
	}

}
