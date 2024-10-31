package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.net.SocketAddress;

public interface ConnectionManager extends LifeCycle {

	Connection connect(SocketAddress socketAddress) throws RemotingException;

	Connection get(SocketAddress socketAddress) throws RemotingException;

	void check(Connection connection) throws RemotingException;

	void close(Connection connection);

	void add(Connection connection);

	Reconnector reconnector();

}
