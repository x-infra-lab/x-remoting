package io.github.xinfra.lab.remoting.connection;

import org.apache.commons.lang3.Validate;

import java.net.SocketAddress;
import java.util.concurrent.Future;

public class ServerConnectionManager extends AbstractConnectionManager {

	@Override
	public Connection get(SocketAddress socketAddress) {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress can not be null");

		ConnectionHolder connectionHolder = connections.get(socketAddress);
		if (connectionHolder == null) {
			return null;
		}
		return connectionHolder.get();
	}

	@Override
	public void reconnect(SocketAddress socketAddress) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void disableReconnect(SocketAddress socketAddress) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void enableReconnect(SocketAddress socketAddress) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Void> asyncReconnect(SocketAddress socketAddress) {
		throw new UnsupportedOperationException();
	}

}
