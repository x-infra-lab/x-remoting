package io.github.xinfra.lab.remoting.connection;

public interface ConnectionEventListener {

	ConnectionEvent interest();

	void onEvent(Connection connection);

}
