package io.github.xinfra.lab.remoting.rpc.heartbeat;

import io.github.xinfra.lab.remoting.connection.Connection;

import java.net.InetSocketAddress;

public interface Heartbeater {

	void triggerHeartBeat(Connection connection);

	void disableHeartBeat(Connection connection);

	void enableHeartBeat(Connection connection);

	void disableHeartBeat(InetSocketAddress socketAddress);

	void enableHeartBeat(InetSocketAddress socketAddress);

}
