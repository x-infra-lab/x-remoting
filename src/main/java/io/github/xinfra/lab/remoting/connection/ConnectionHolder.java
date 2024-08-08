package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;

import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionHolder {

	@AccessForTest
	protected CopyOnWriteArrayList<Connection> connections = new CopyOnWriteArrayList<>();

	private ConnectionSelectStrategy connectionSelectStrategy;

	public ConnectionHolder(ConnectionSelectStrategy connectionSelectStrategy) {
		this.connectionSelectStrategy = connectionSelectStrategy;
	}

	public Connection get() {
		return connectionSelectStrategy.select(connections);
	}

	public void add(Connection connection) {
		connections.add(connection);
	}

	public void removeAndClose(Connection connection) {
		connections.remove(connection);
		connection.close();
	}

	public boolean isEmpty() {
		return connections.isEmpty();
	}

	public void removeAndCloseAll() {
		for (Connection connection : connections) {
			connections.remove(connection);
			connection.close();
		}
	}

	public int size() {
		return connections.size();
	}

}
