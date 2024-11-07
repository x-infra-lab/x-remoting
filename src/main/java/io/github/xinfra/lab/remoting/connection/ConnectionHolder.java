package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionHolder implements Closeable {

	@AccessForTest
	protected CopyOnWriteArrayList<Connection> connections = new CopyOnWriteArrayList<>();

	private ConnectionSelectStrategy connectionSelectStrategy;

	public ConnectionHolder(ConnectionSelectStrategy connectionSelectStrategy) {
		this.connectionSelectStrategy = connectionSelectStrategy;
	}

	public Connection get() {
		List<Connection> snapshot = new ArrayList<>(connections);
		if (snapshot.size() > 0) {
			return connectionSelectStrategy.select(new ArrayList<>(connections));
		}
		return null;
	}

	public void add(Connection connection) {
		connections.addIfAbsent(connection);
	}

	public boolean invalidate(Connection connection) {
		connection.close();
		return connections.remove(connection);
	}

	public boolean isEmpty() {
		return connections.isEmpty();
	}

	public int size() {
		return connections.size();
	}

	@Override
	public void close() {
		for (Connection connection : connections) {
			connections.remove(connection);
			connection.close();
		}
	}

}
