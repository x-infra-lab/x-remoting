package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.Validate;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractConnectionManager extends AbstractLifeCycle implements ConnectionManager {

	protected ConcurrentHashMap<InetSocketAddress, Connections> connectionsMap = new ConcurrentHashMap<>();

	protected ConnectionFactory connectionFactory;

	protected ConnectionSelectStrategy connectionSelectStrategy = new RoundRobinConnectionSelectStrategy();

	protected ConnectionManagerConfig config = ConnectionManagerConfig.defaults();

	private ConnectionEventProcessor connectionEventProcessor = new DefaultConnectionEventProcessor();

	public AbstractConnectionManager() {
	}

	public AbstractConnectionManager(ConnectionManagerConfig config) {
		this.config = config;
	}

	@Override
	public void disconnect(InetSocketAddress socketAddress) {
		ensureStarted();
		Validate.notNull(socketAddress, "socket address can not be null");
		if (reconnector() != null) {
			reconnector().cancel(socketAddress);
		}

		Connections connections = connectionsMap.remove(socketAddress);
		if (connections != null) {
			connections.close();
		}
		log.info("Disconnect connection for address: {}", socketAddress);
	}

	@Override
	public void check(Connection connection) throws RemotingException {
		ensureStarted();
		Validate.notNull(connection, "connection can not be null");

		if (connection.getChannel() == null || !connection.getChannel().isActive() || connection.isClosed()) {
			this.close(connection);
			throw new RemotingException("Check connection failed for address: " + connection.remoteAddress());
		}
		if (!connection.getChannel().isWritable()) {
			// No remove. Most of the time it is unwritable temporarily.
			throw new RemotingException(
					"Check connection failed for address: " + connection.remoteAddress() + ", maybe write overflow!");
		}
	}

	@Override
	public void close(Connection connection) {
		ensureStarted();
		Validate.notNull(connection, "connection can not be null");

		InetSocketAddress socketAddress = (InetSocketAddress) connection.remoteAddress();
		Connections connections = connectionsMap.get(socketAddress);
		if (connections == null) {
			connection.close();
			return;
		}

		if (connections.invalidate(connection)) {
			Reconnector r = reconnector();
			if (r != null && r.isStarted()) {
				r.onUnhealthy(socketAddress);
			}
		}
		// Lazily drop the empty bucket. Only remove if the mapping still points to the
		// same Connections instance — protects against a concurrent add() that re-created
		// the entry.
		if (connections.isEmpty()) {
			connectionsMap.remove(socketAddress, connections);
		}
	}

	@Override
	public void add(Connection connection) {
		ensureStarted();
		Validate.notNull(connection, "connection can not be null");

		InetSocketAddress socketAddress = (InetSocketAddress) connection.remoteAddress();
		connectionsMap.compute(socketAddress, (k, existing) -> {
			Connections cs = (existing != null) ? existing : new Connections(connectionSelectStrategy);
			cs.add(connection);
			return cs;
		});
	}

	@Override
	public ConnectionEventProcessor connectionEventProcessor() {
		return connectionEventProcessor;
	}

	@Override
	public void startup() {
		super.startup();
		connectionEventProcessor.startup();
	}

	@Override
	public void shutdown() {
		List<InetSocketAddress> addresses = new ArrayList<>(connectionsMap.keySet());
		for (InetSocketAddress address : addresses) {
			disconnect(address);
		}
		super.shutdown();
		connectionEventProcessor.shutdown();
	}

	protected Connections createConnections(InetSocketAddress socketAddress) {
		return connectionsMap.computeIfAbsent(socketAddress, k -> new Connections(connectionSelectStrategy));
	}

	protected void createConnection(InetSocketAddress socketAddress, Connections connections, int size)
			throws RemotingException {
		// Serialize fill attempts for the same address; other addresses are unaffected.
		synchronized (connections) {
			while (connections.size() < size) {
				if (connections.isClosed()) {
					throw new RemotingException(
							"Connections to " + socketAddress + " was closed concurrently during connect");
				}
				Connection connection = connectionFactory.create(socketAddress);
				connections.add(connection);
			}
		}
	}

}
