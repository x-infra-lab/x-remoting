package io.github.xinfra.lab.remoting.connection;

import java.io.Closeable;
import java.net.SocketAddress;

public interface Heartbeater extends Closeable {

	void triggerHeartBeat(Connection connection);

	void disableHeartBeat(Connection connection);

	void enableHeartBeat(Connection connection);

	void disableHeartBeat(SocketAddress socketAddress);

	void enableHeartBeat(SocketAddress socketAddress);

}
