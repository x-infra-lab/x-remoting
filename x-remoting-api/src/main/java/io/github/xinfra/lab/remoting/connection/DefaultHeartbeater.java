package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.client.RemotingClient;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

@Slf4j
public class DefaultHeartbeater implements Heartbeater {

	private RemotingClient remotingClient;

	public DefaultHeartbeater() {
		this.remotingClient = new RemotingClient() {
		};
	}

	@Override
	public void triggerHeartBeat(Connection connection) {
		int heartbeatFailCount = connection.getHeartbeatFailCnt();
		if (heartbeatFailCount > connection.getHeartbeatMaxFailCount()) {
			connection.close();
			log.error("close connection after heartbeat fail {} times. remote address:{}", heartbeatFailCount,
					connection.remoteAddress());
			return;
		}

		Protocol protocol = connection.getProtocol();
		RequestMessage heartbeatRequestMessage = protocol.messageFactory()
			.createRequest(IDGenerator.nextRequestId(), MessageType.heartbeat, SerializationType.Hession);
		remotingClient.asyncCall(heartbeatRequestMessage, connection, connection.getHeartbeatTimeoutMills(),
				responseMessage -> {

					if (responseMessage.status() == ResponseStatus.OK) {
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
	public void disableHeartBeat(Connection connection) {
		// todo
	}

	@Override
	public void enableHeartBeat(Connection connection) {
		// todo
	}

	@Override
	public void disableHeartBeat(SocketAddress socketAddress) {
		// todo
	}

	@Override
	public void enableHeartBeat(SocketAddress socketAddress) {
		// todo
	}

}
