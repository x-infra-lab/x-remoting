package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractConnectionManager implements ConnectionManager {

    private ConnectionConfig config;

    private Map<Endpoint, ConnectionHolder> connections = new ConcurrentHashMap<>();

    protected ConnectionFactory connectionFactory;

    private ConnectionSelectStrategy connectionSelectStrategy = new RoundRobinConnectionSelectStrategy();

    public AbstractConnectionManager() {
        this.config = new ConnectionConfig();
    }

    @Override
    public Connection getOrCreateIfAbsent(Endpoint endpoint) throws RemotingException {
        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            synchronized (this) {
                connectionHolder = connections.get(endpoint);
                if (connectionHolder == null) {
                    connectionHolder = createConnectionPool(endpoint);
                    createConnectionForPool(endpoint, connectionHolder);
                }
            }
        }

        return connectionHolder.get();
    }

    @Override
    public void check(Connection connection) throws RemotingException {
        if (connection == null) {
            throw new RemotingException("Connection is null when do check!");
        }
        if (connection.getChannel() == null || !connection.getChannel().isActive()) {
            this.remove(connection);
            throw new RemotingException("Check connection failed for address: "
                    + connection.getEndpoint());
        }
        if (!connection.getChannel().isWritable()) {
            // No remove. Most of the time it is unwritable temporarily.
            throw new RemotingException("Check connection failed for address: "
                    + connection.getEndpoint() + ", maybe write overflow!");
        }
    }

    @Override
    public void remove(Connection connection) {
        Endpoint endpoint = connection.getEndpoint();
        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            connection.close();
        } else {
            connectionHolder.remove(connection);
            if (connectionHolder.isEmpty()) {
                connections.remove(endpoint);
            }
        }
    }

    @Override
    public Connection get(Endpoint endpoint) {
        // TODO
        return null;
    }

    @Override
    public void add(Connection connection) {
        // TODO
    }

    private ConnectionHolder createConnectionPool(Endpoint endpoint) throws RemotingException {
        ConnectionHolder connectionHolder = new ConnectionHolder(connectionSelectStrategy);
        connections.put(endpoint, connectionHolder);
        return connectionHolder;
    }

    private void createConnectionForPool(Endpoint endpoint, ConnectionHolder connectionHolder) throws RemotingException {
        for (int i = 0; i < config.getConnNum(); i++) {
            Connection connection = connectionFactory.create(endpoint, config);
            connectionHolder.add(connection);
        }
    }
}
