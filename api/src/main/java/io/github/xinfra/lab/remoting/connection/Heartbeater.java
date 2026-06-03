package io.github.xinfra.lab.remoting.connection;

import java.net.InetSocketAddress;

public interface Heartbeater {

	void triggerHeartBeat(Connection connection);

	void disableHeartBeat(Connection connection);

	void enableHeartBeat(Connection connection);

	void disableHeartBeat(InetSocketAddress socketAddress);

	void enableHeartBeat(InetSocketAddress socketAddress);

}
