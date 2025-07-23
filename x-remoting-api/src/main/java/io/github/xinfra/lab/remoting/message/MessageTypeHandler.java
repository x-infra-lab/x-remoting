package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;

public interface MessageTypeHandler {

	MessageType messageType();

	void handleMessage(Connection connection, Message msg);

}
