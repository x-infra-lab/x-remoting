package io.github.xinfra.lab.remoting.connection;

public interface ConnectionEventListener {

	void onEvent(ConnectionEvent connectionEvent, Connection connection);

}
