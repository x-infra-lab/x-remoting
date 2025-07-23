package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;

import java.util.concurrent.Executor;

public class HeartbeatMessageTypeHandler implements MessageTypeHandler {

	@Override
	public MessageType messageType() {
		return MessageType.heartbeat;
	}

	@Override
	public void handleMessage(Connection connection, Message msg) {
		// todo
	}

}
