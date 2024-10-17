package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.LifeCycle;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.net.SocketAddress;
import java.util.concurrent.Future;

public interface Reconnector extends LifeCycle {

	void reconnect(SocketAddress socketAddress) throws RemotingException;

	void disableReconnect(SocketAddress socketAddress);

	void enableReconnect(SocketAddress socketAddress);

	Future<Void> asyncReconnect(SocketAddress socketAddress);

}
