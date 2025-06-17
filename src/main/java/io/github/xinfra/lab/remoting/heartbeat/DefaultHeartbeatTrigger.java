package io.github.xinfra.lab.remoting.heartbeat;

import io.github.xinfra.lab.remoting.client.BaseRemoting;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;



@Slf4j
public class DefaultHeartbeatTrigger implements HeartbeatTrigger {

	private int heartbeatTimeoutMills = 1000;

	private int maxFailCount = 3;

	private  Protocol protocol;

	private  BaseRemoting baseRemoting;

	public DefaultHeartbeatTrigger(Protocol protocol) {
		this.protocol = protocol;
		this.baseRemoting = new BaseRemoting(protocol);
	}

	@Override
	public void triggerHeartBeat(Connection connection) {
		int heartbeatFailCount = connection.getHeartbeatFailCnt();
		if (heartbeatFailCount > maxFailCount) {
			connection.close();
			log.error("close connection after heartbeat fail {} times. remote address:{}", heartbeatFailCount,
					connection.remoteAddress());
			return;
		}

		RequestMessage heartbeatRequestMessage = protocol.messageFactory().createHeartbeatRequestMessage();
		baseRemoting.asyncCall(heartbeatRequestMessage, connection, heartbeatTimeoutMills, responseMessage -> {

			if (responseMessage.isOk()) {
				log.debug("heartbeat success. remote address:{}", connection.remoteAddress());
				connection.setHeartbeatFailCnt(0);
			}
			else {
				int failCount = connection.getHeartbeatFailCnt() + 1;
				log.warn("heartbeat fail {} times. remote address:{}", failCount, connection.remoteAddress());
				connection.setHeartbeatFailCnt(failCount);
			}

		});

	}

	@Override
	public void setHeartbeatMaxFailCount(int failCount) {
		Validate.isTrue(failCount >= 0, "failCount must >= 0");
		this.maxFailCount = failCount;
	}

	@Override
	public void setHeartbeatTimeoutMills(int timeoutMills) {
		Validate.isTrue(timeoutMills > 0, "timeoutMills must > 0");
		this.heartbeatTimeoutMills = timeoutMills;
	}

}
