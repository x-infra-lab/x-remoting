package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractConnectionManager extends AbstractLifeCycle implements ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractConnectionManager.class);
    @AccessForTest
    protected Map<Endpoint, ConnectionHolder> connections = new ConcurrentHashMap<>();

    protected ConnectionFactory connectionFactory;

    private ConnectionSelectStrategy connectionSelectStrategy = new RoundRobinConnectionSelectStrategy();

    protected ConnectionManagerConfig config = new ConnectionManagerConfig();


    public AbstractConnectionManager() {
    }

    public AbstractConnectionManager(ConnectionManagerConfig config) {
        this.config = config;
    }

    @Override
    public synchronized Connection getOrCreateIfAbsent(Endpoint endpoint) throws RemotingException {
        ensureStarted();
        Validate.notNull(endpoint, "endpoint can not be null");

        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            connectionHolder = createConnectionHolder(endpoint);
            createConnectionForHolder(endpoint, connectionHolder, config.getConnectionNumPreEndpoint());
        }

        return connectionHolder.get();
    }

    @Override
    public void check(Connection connection) throws RemotingException {
        ensureStarted();
        Validate.notNull(connection, "connection can not be null");

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
    public synchronized void removeAndClose(Connection connection) {
        ensureStarted();
        Validate.notNull(connection, "connection can not be null");

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
        ensureStarted();
        Validate.notNull(endpoint, "endpoint can not be null");

        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            return null;
        }
        return connectionHolder.get();
    }

    @Override
    public synchronized void add(Connection connection) {
        ensureStarted();
        Validate.notNull(connection, "connection can not be null");

        Endpoint endpoint = connection.getEndpoint();
        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            connectionHolder = createConnectionHolder(endpoint);
            connectionHolder.add(connection);
        } else {
            connectionHolder.add(connection);
        }
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        for (Map.Entry<Endpoint, ConnectionHolder> entry : connections.entrySet()) {
            Endpoint endpoint = entry.getKey();
            ConnectionHolder connectionHolder = entry.getValue();
            connectionHolder.removeAndCloseAll();
            connections.remove(endpoint);
        }

    }

    protected ConnectionHolder createConnectionHolder(Endpoint endpoint) {
        ConnectionHolder connectionHolder = new ConnectionHolder(connectionSelectStrategy);
        connections.put(endpoint, connectionHolder);
        return connectionHolder;
    }

    protected void createConnectionForHolder(Endpoint endpoint, ConnectionHolder connectionHolder, int size) throws RemotingException {
        for (int i = 0; i < size; i++) {
            Connection connection = connectionFactory.create(endpoint);
            connectionHolder.add(connection);
        }
    }


}
