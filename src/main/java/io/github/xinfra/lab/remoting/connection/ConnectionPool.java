package io.github.xinfra.lab.remoting.connection;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConnectionPool {

    private CopyOnWriteArrayList<Connection> connections = new CopyOnWriteArrayList<>();
    private ConnectionSelectStrategy connectionSelectStrategy;

    public ConnectionPool(ConnectionSelectStrategy connectionSelectStrategy) {
        this.connectionSelectStrategy = connectionSelectStrategy;
    }

    public Connection get() {
        return connectionSelectStrategy.select(connections);
    }

    public void add(Connection connection) {
        connections.add(connection);
    }

    public void remove(Connection connection) {
        connections.remove(connection);
        connection.close();
    }

    public boolean isEmpty() {
        return connections.isEmpty();
    }
}
