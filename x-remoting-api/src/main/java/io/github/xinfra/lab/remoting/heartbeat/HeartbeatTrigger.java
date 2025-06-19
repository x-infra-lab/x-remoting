package io.github.xinfra.lab.remoting.heartbeat;

import io.github.xinfra.lab.remoting.connection.Connection;

public interface HeartbeatTrigger {

	void triggerHeartBeat(Connection connection);

	void setHeartbeatMaxFailCount(int failCount);

	void setHeartbeatTimeoutMills(int timeoutMills);

}
