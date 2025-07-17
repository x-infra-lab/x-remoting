package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import java.util.concurrent.Executor;

public interface MessageTypeHandler {

	MessageType messageType();

	void handleMessage(Connection connection, Message msg);

	Executor executor();

}
