package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.LifeCycle;

public interface ConnectionEventProcessor extends LifeCycle {

	void handleEvent(ConnectionEvent event, Connection connection);

	void addConnectionEventListener(ConnectionEventListener listener);

}
