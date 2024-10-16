package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.net.SocketAddress;
import java.util.concurrent.Future;

public interface ConnectionManager extends LifeCycle {

	Connection get(SocketAddress socketAddress) throws RemotingException;

	void check(Connection connection) throws RemotingException;

	void invalidate(Connection connection);

	void add(Connection connection);

	void reconnect(SocketAddress socketAddress) throws RemotingException;

	void disableReconnect(SocketAddress socketAddress);

	void enableReconnect(SocketAddress socketAddress);

	Future<Void> asyncReconnect(SocketAddress socketAddress);

}
