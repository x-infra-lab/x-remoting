package io.github.xinfra.lab.remoting.connection;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Connections implements Closeable {

	protected CopyOnWriteArrayList<Connection> connections = new CopyOnWriteArrayList<>();

	private final ConnectionSelectStrategy connectionSelectStrategy;

	private volatile boolean closed = false;

	public Connections(ConnectionSelectStrategy connectionSelectStrategy) {
		this.connectionSelectStrategy = connectionSelectStrategy;
	}

	public Connection get() {
		List<Connection> snapshot = new ArrayList<>(connections);
		if (!snapshot.isEmpty()) {
			return connectionSelectStrategy.select(snapshot);
		}
		return null;
	}

	/**
	 * Add a connection. If this {@code Connections} has already been closed (or becomes
	 * closed concurrently), the connection is closed instead of being added.
	 */
	public void add(Connection connection) {
		if (closed) {
			connection.close();
			return;
		}
		connections.addIfAbsent(connection);
		// Double-check: close() may have flipped between the check and the add.
		if (closed && connections.remove(connection)) {
			connection.close();
		}
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

	public boolean isClosed() {
		return closed;
	}

	@Override
	public void close() {
		closed = true;
		List<Connection> snapshot = new ArrayList<>(connections);
		connections.clear();
		for (Connection connection : snapshot) {
			connection.close();
		}
	}

}
