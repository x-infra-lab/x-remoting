package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.Protocol;

import java.net.SocketAddress;
import java.util.concurrent.Future;


public interface ConnectionManager extends LifeCycle {

    Connection getOrCreateIfAbsent(SocketAddress socketAddress) throws RemotingException;

    Connection get(SocketAddress socketAddress);

    void check(Connection connection) throws RemotingException;

    void removeAndClose(Connection connection);

    void add(Connection connection);

    void reconnect(SocketAddress socketAddress) throws RemotingException;

    void disableReconnect(SocketAddress socketAddress);

    void enableReconnect(SocketAddress socketAddress);

    Future<Void> asyncReconnect(SocketAddress socketAddress);

}
