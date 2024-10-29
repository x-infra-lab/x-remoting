package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.net.SocketAddress;

public interface Reconnector extends LifeCycle {

	void disableReconnect(SocketAddress socketAddress);

	void enableReconnect(SocketAddress socketAddress);

	void reconnect(SocketAddress socketAddress) throws RemotingException;

}
