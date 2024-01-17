package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractConnectionManager implements ConnectionManager {

    private Map<Endpoint, ConnectionPool> pools = new ConcurrentHashMap<>();

    protected ConnectionFactory connectionFactory;

    private ConnectionSelectStrategy connectionSelectStrategy = new RoundRobinConnectionSelectStrategy();


    @Override
    public Connection getOrCreateIfAbsent(Endpoint endpoint) throws RemotingException {
        ConnectionPool connectionPool = pools.get(endpoint);
        if (connectionPool == null) {
            synchronized (this) {
                connectionPool = pools.get(endpoint);
                if (connectionPool == null) {
                    connectionPool = createConnectionPool(endpoint);
                    createConnectionForPool(endpoint, connectionPool);
                }
            }
        }

        return connectionPool.get();
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
    public void remove(Connection connection)  {
        Endpoint endpoint = connection.getEndpoint();
        ConnectionPool connectionPool = pools.get(endpoint);
        if (connectionPool == null) {
            connection.close();
        } else {
            connectionPool.remove(connection);
            if (connectionPool.isEmpty()) {
                pools.remove(endpoint);
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

    private ConnectionPool createConnectionPool(Endpoint endpoint) throws RemotingException {
        ConnectionPool connectionPool = new ConnectionPool(connectionSelectStrategy);
        pools.put(endpoint, connectionPool);
        return connectionPool;
    }

    private void createConnectionForPool(Endpoint endpoint, ConnectionPool connectionPool) throws RemotingException {
        for (int i = 0; i < endpoint.getConnNum(); i++) {
            Connection connection = connectionFactory.create(endpoint);
            connectionPool.add(connection);
        }
    }
}
