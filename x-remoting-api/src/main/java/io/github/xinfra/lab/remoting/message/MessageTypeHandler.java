package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;

public interface MessageTypeHandler<T extends  Message> {

	MessageType messageType();

	void handleMessage(Connection connection, T msg);

}
