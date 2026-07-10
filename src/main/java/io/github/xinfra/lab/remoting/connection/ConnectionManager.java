package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.net.InetSocketAddress;

public interface ConnectionManager extends LifeCycle {

	Connection connect(InetSocketAddress socketAddress) throws RemotingException;

	void disconnect(InetSocketAddress socketAddress);

	Connection get(InetSocketAddress socketAddress) throws RemotingException;

	void check(Connection connection) throws RemotingException;

	void close(Connection connection);

	void add(Connection connection);

	Reconnector reconnector();

	ConnectionEventProcessor connectionEventProcessor();

}
