package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.client.Call;
import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DefaultHeartbeater implements Heartbeater {

	private final Set<Connection> disabledConnections = ConcurrentHashMap.newKeySet();

	private final Set<InetSocketAddress> disabledSocketAddresses = ConcurrentHashMap.newKeySet();

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
		HeartbeatState hbState = connection.getHeartbeatState();
		int heartbeatFailCount = hbState.getFailCount();
		if (heartbeatFailCount >= hbState.getMaxFailCount()) {
			connection.close();
			log.error("close connection after heartbeat fail {} times. remote address:{}", heartbeatFailCount,
					connection.remoteAddress());
			return;
		}

		Protocol protocol = connection.getProtocol();
		RequestMessage heartbeatRequestMessage = protocol.getMessageFactory()
			.createHeartbeatRequest(IDGenerator.nextRequestId(), SerializationType.Hession);

		CallOptions callOptions = CallOptions.builder().timeoutMills(hbState.getTimeoutMills()).build();
		call.asyncCall(heartbeatRequestMessage, connection, callOptions, responseMessage -> {

			if (responseMessage.getResponseStatus() == ResponseStatus.OK) {
				log.debug("heartbeat success. remote address:{}", connection.remoteAddress());
				connection.getHeartbeatState().resetFailCount();
			}
			else {
				int failCount = connection.getHeartbeatState().incrementFailCount();
				log.warn("heartbeat fail {} times. remote address:{}", failCount, connection.remoteAddress());
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
	public void disableHeartBeat(InetSocketAddress socketAddress) {
		disabledSocketAddresses.add(socketAddress);
	}

	@Override
	public void enableHeartBeat(InetSocketAddress socketAddress) {
		disabledSocketAddresses.remove(socketAddress);
	}

}
