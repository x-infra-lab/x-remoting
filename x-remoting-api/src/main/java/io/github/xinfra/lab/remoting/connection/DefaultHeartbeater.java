package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.client.Call;
import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class DefaultHeartbeater implements Heartbeater {

	private Set<Connection> disabledConnections = new HashSet<>();

	private Set<SocketAddress> disabledSocketAddresses = new HashSet<>();

	private Call call;

	public DefaultHeartbeater() {
		this.call = new Call() {
		};
	}

	@Override
	public void triggerHeartBeat(Connection connection) {
		if (disabledConnections.contains(connection)) {
			log.debug("heartbeat is disabled. connection:{}", connection);
			return;
		}
		if (disabledSocketAddresses.contains(connection.remoteAddress())) {
			log.debug("heartbeat is disabled for socket address:{}", connection.remoteAddress());
			return;
		}
		int heartbeatFailCount = connection.getHeartbeatFailCnt();
		if (heartbeatFailCount > connection.getHeartbeatMaxFailCount()) {
			connection.close();
			log.error("close connection after heartbeat fail {} times. remote address:{}", heartbeatFailCount,
					connection.remoteAddress());
			return;
		}

		Protocol protocol = connection.getProtocol();
		RequestMessage heartbeatRequestMessage = protocol.messageFactory()
			.createHeartbeatRequest(IDGenerator.nextRequestId(), SerializationType.Hession);

		CallOptions callOptions = new CallOptions();
		callOptions.setTimeoutMills(connection.getHeartbeatTimeoutMills());
		call.asyncCall(heartbeatRequestMessage, connection, callOptions, responseMessage -> {

			if (responseMessage.responseStatus() == ResponseStatus.OK) {
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
		disabledConnections.add(connection);
	}

	@Override
	public void enableHeartBeat(Connection connection) {
		disabledConnections.remove(connection);
	}

	@Override
	public void disableHeartBeat(SocketAddress socketAddress) {
		disabledSocketAddresses.add(socketAddress);
	}

	@Override
	public void enableHeartBeat(SocketAddress socketAddress) {
		disabledSocketAddresses.remove(socketAddress);
	}

}
