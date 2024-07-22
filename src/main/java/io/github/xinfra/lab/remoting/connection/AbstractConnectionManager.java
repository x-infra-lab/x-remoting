package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.annotation.OnlyForTest;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public abstract class AbstractConnectionManager extends AbstractLifeCycle implements ConnectionManager {

    private static final Logger log = LoggerFactory.getLogger(AbstractConnectionManager.class);
    @OnlyForTest
    @Getter
    protected Map<Endpoint, ConnectionHolder> connections = new ConcurrentHashMap<>();

    protected ConnectionFactory connectionFactory;

    private ConnectionSelectStrategy connectionSelectStrategy = new RoundRobinConnectionSelectStrategy();

    private ConnectionManagerConfig config = new ConnectionManagerConfig();

    private ExecutorService reconnector = new ThreadPoolExecutor(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(1024),
            new NamedThreadFactory("Reconnector-Worker"));

    private Set<Endpoint> disableReconnectEndpoints = new CopyOnWriteArraySet<>();


    public AbstractConnectionManager() {
    }

    public AbstractConnectionManager(ConnectionManagerConfig config) {
        this.config = config;
    }

    @Override
    public synchronized Connection getOrCreateIfAbsent(Endpoint endpoint) throws RemotingException {
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
        Validate.notNull(endpoint, "endpoint can not be null");

        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            return null;
        }
        return connectionHolder.get();
    }

    @Override
    public synchronized void add(Connection connection) {
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

    private ConnectionHolder createConnectionHolder(Endpoint endpoint) {
        ConnectionHolder connectionHolder = new ConnectionHolder(connectionSelectStrategy);
        connections.put(endpoint, connectionHolder);
        return connectionHolder;
    }

    private void createConnectionForHolder(Endpoint endpoint, ConnectionHolder connectionHolder, int size) throws RemotingException {
        for (int i = 0; i < size; i++) {
            Connection connection = connectionFactory.create(endpoint);
            connectionHolder.add(connection);
        }
    }

    @Override
    public synchronized void reconnect(Endpoint endpoint) throws RemotingException {
        if (disableReconnectEndpoints.contains(endpoint)) {
            log.warn("endpoint:{} is disable to reconnect", endpoint);
            return;
        }
        ConnectionHolder connectionHolder = connections.get(endpoint);
        if (connectionHolder == null) {
            connectionHolder = createConnectionHolder(endpoint);
            createConnectionForHolder(endpoint, connectionHolder, config.getConnectionNumPreEndpoint());
        } else {
            int needCreateNum = config.getConnectionNumPreEndpoint() - connectionHolder.size();
            if (needCreateNum > 0) {
                createConnectionForHolder(endpoint, connectionHolder, needCreateNum);
            }
        }
    }

    @Override
    public void disableReconnect(Endpoint endpoint) {
        disableReconnectEndpoints.add(endpoint);
    }

    @Override
    public void enableReconnect(Endpoint endpoint) {
        disableReconnectEndpoints.remove(endpoint);
    }

    @Override
    public Future<Void> asyncReconnect(Endpoint endpoint) {
        Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                try {
                    reconnect(endpoint);
                } catch (Exception e) {
                    log.warn("reconnect endpoint:{} fail", endpoint, e);
                    throw e;
                }
                return null;
            }
        };
        return reconnector.submit(callable);
    }
}
