package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractConnectionManager extends AbstractLifeCycle implements ConnectionManager {

    @AccessForTest
    protected Map<SocketAddress, ConnectionHolder> connections = new ConcurrentHashMap<>();

    protected ConnectionFactory connectionFactory;

    private ConnectionSelectStrategy connectionSelectStrategy = new RoundRobinConnectionSelectStrategy();

    protected ConnectionManagerConfig config = new ConnectionManagerConfig();


    public AbstractConnectionManager() {
    }

    public AbstractConnectionManager( ConnectionManagerConfig config) {
        this.config = config;
    }


    @Override
    public synchronized Connection getOrCreateIfAbsent(SocketAddress socketAddress) throws RemotingException {
        ensureStarted();
        Validate.notNull(socketAddress, "socketAddress can not be null");

        ConnectionHolder connectionHolder = connections.get(socketAddress);
        if (connectionHolder == null) {
            connectionHolder = createConnectionHolder(socketAddress);
            createConnectionForHolder(socketAddress, connectionHolder, config.getConnectionNumPreEndpoint());
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
                    + connection.remoteAddress());
        }
        if (!connection.getChannel().isWritable()) {
            // No remove. Most of the time it is unwritable temporarily.
            throw new RemotingException("Check connection failed for address: "
                    + connection.remoteAddress() + ", maybe write overflow!");
        }
    }

    @Override
    public synchronized void removeAndClose(Connection connection) {
        ensureStarted();
        Validate.notNull(connection, "connection can not be null");

        SocketAddress socketAddress = connection.remoteAddress();
        ConnectionHolder connectionHolder = connections.get(socketAddress);
        if (connectionHolder == null) {
            connection.close();
        } else {
            connectionHolder.removeAndClose(connection);
            if (connectionHolder.isEmpty()) {
                connections.remove(socketAddress);
            }
        }
    }

    @Override
    public Connection get(SocketAddress socketAddress) {
        ensureStarted();
        Validate.notNull(socketAddress, "socketAddress can not be null");

        ConnectionHolder connectionHolder = connections.get(socketAddress);
        if (connectionHolder == null) {
            return null;
        }
        return connectionHolder.get();
    }

    @Override
    public synchronized void add(Connection connection) {
        ensureStarted();
        Validate.notNull(connection, "connection can not be null");

        SocketAddress socketAddress = connection.remoteAddress();
        ConnectionHolder connectionHolder = connections.get(socketAddress);
        if (connectionHolder == null) {
            connectionHolder = createConnectionHolder(socketAddress);
            connectionHolder.add(connection);
        } else {
            connectionHolder.add(connection);
        }
    }

    @Override
    public synchronized void shutdown() {
        super.shutdown();
        for (Map.Entry<SocketAddress, ConnectionHolder> entry : connections.entrySet()) {
            SocketAddress socketAddress = entry.getKey();
            ConnectionHolder connectionHolder = entry.getValue();
            connectionHolder.removeAndCloseAll();
            connections.remove(socketAddress);
        }

    }

    protected ConnectionHolder createConnectionHolder(SocketAddress socketAddress) {
        ConnectionHolder connectionHolder = new ConnectionHolder(connectionSelectStrategy);
        connections.put(socketAddress, connectionHolder);
        return connectionHolder;
    }

    protected void createConnectionForHolder(SocketAddress socketAddress, ConnectionHolder connectionHolder, int size) throws RemotingException {
        for (int i = 0; i < size; i++) {
            Connection connection = connectionFactory.create(socketAddress);
            connectionHolder.add(connection);
        }
    }


}
