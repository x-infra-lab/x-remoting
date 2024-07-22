package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.util.concurrent.Future;


public interface ConnectionManager extends LifeCycle {

    Connection getOrCreateIfAbsent(Endpoint endpoint) throws RemotingException;

    Connection get(Endpoint endpoint);

    void check(Connection connection) throws RemotingException;

    void removeAndClose(Connection connection);

    void add(Connection connection);

    void reconnect(Endpoint endpoint) throws RemotingException;

    void disableReconnect(Endpoint endpoint);

    void enableReconnect(Endpoint endpoint);

    Future<Void> asyncReconnect(Endpoint endpoint);
}
