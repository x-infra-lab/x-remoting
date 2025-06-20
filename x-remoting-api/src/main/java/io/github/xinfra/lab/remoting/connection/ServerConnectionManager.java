package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import org.apache.commons.lang3.Validate;

import java.net.SocketAddress;

public class ServerConnectionManager extends AbstractConnectionManager {

	@Override
	public Connection connect(SocketAddress socketAddress) throws RemotingException {
		throw new UnsupportedOperationException("ServerConnectionManager not support connect");
	}

	@Override
	public Connection get(SocketAddress socketAddress) {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress can not be null");

		Connections connections = this.connections.get(socketAddress);
		if (connections == null) {
			return null;
		}
		return connections.get();
	}

	@Override
	public Reconnector reconnector() {
		return null;
	}

	@Override
	public Heartbeater heartbeatTrigger() {
		return null;
	}

}
