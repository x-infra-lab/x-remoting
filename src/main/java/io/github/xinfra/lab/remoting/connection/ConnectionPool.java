package io.github.xinfra.lab.remoting.connection;

public interface ConnectionPool {
    Connection select(ConnectionSelectStrategy connectionSelectStrategy);

    void add(Connection connection);

    void removeAndClose(Connection connection);

    void removeAndCloseAll();

    boolean isEmpty();

    int size();
}
