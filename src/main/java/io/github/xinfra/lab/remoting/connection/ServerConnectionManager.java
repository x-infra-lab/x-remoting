package io.github.xinfra.lab.remoting.connection;

import org.apache.commons.lang3.Validate;
import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ServerConnectionManager extends AbstractConnectionManager {

	@Override
	public Connection connect(InetSocketAddress socketAddress) throws RemotingException {
		throw new UnsupportedOperationException("ServerConnectionManager not support connect");
	}

	@Override
	public Connection get(InetSocketAddress socketAddress) {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress can not be null");

		Connections connections = this.connectionsMap.get(socketAddress);
		if (connections == null) {
			return null;
		}
		return connections.get();
	}

	public void forEachConnection(Consumer<Connection> action) {
		for (Connections conns : connectionsMap.values()) {
			for (Connection conn : new ArrayList<>(conns.connections)) {
				action.accept(conn);
			}
		}
	}

	@Override
	public Reconnector reconnector() {
		return null;
	}

}
