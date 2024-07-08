package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.annotation.OnlyForTest;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractConnectionManager extends AbstractLifeCycle implements ConnectionManager  {

    @OnlyForTest
    @Getter
    public Map<Endpoint, ConnectionHolder> connections = new ConcurrentHashMap<>();

    protected ConnectionFactory connectionFactory;

    private ConnectionSelectStrategy connectionSelectStrategy = new RoundRobinConnectionSelectStrategy();

    private ConnectionManagerConfig config = new ConnectionManagerConfig();

    public AbstractConnectionManager() {
    }

    public AbstractConnectionManager(ConnectionManagerConfig config) {
        this.config = config;
    }

    @Override
    public Connection getOrCreateIfAbsent(Endpoint endpoint) throws RemotingException {
        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            synchronized (this) {
                connectionHolder = connections.get(endpoint);
                if (connectionHolder == null) {
                    connectionHolder = createConnectionHolder(endpoint);
                    createConnectionForHolder(endpoint, connectionHolder);
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
            this.removeAndClose(connection);
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
    public void removeAndClose(Connection connection) {
        if (connection == null) {
            return;
        }
        Endpoint endpoint = connection.getEndpoint();
        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            connection.close();
        } else {
            connectionHolder.removeAndClose(connection);
            if (connectionHolder.isEmpty()) {
                connections.remove(endpoint);
            }
        }
    }

    @Override
    public Connection get(Endpoint endpoint) {
        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            return null;
        }
        return connectionHolder.get();
    }

    @Override
    public void add(Connection connection) {
        Validate.notNull(connection, "connection can not be null");

        Endpoint endpoint = connection.getEndpoint();

        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            synchronized (this) {
                connectionHolder = connections.get(endpoint);
                if (connectionHolder == null) {
                    connectionHolder = createConnectionHolder(endpoint);
                    connectionHolder.add(connection);
                }
            }
        } else {
            connectionHolder.add(connection);
        }
    }

    private ConnectionHolder createConnectionHolder(Endpoint endpoint) {
        ConnectionHolder connectionHolder = new ConnectionHolder(connectionSelectStrategy);
        connections.put(endpoint, connectionHolder);
        return connectionHolder;
    }

    private void createConnectionForHolder(Endpoint endpoint, ConnectionHolder connectionHolder) throws RemotingException {
        for (int i = 0; i < config.getConnectionNumPreEndpoint(); i++) {
            Connection connection = connectionFactory.create(endpoint);
            connectionHolder.add(connection);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        // todo
    }
}
