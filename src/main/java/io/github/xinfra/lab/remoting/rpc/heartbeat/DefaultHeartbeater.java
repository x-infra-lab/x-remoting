package io.github.xinfra.lab.remoting.rpc.heartbeat;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.rpc.client.Call;
import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.rpc.client.IDGenerator;
import io.github.xinfra.lab.remoting.rpc.message.RequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.rpc.protocol.RpcProtocol;
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
		HeartbeatState hbState = HeartbeatState.getOrCreate(connection);
		int heartbeatFailCount = hbState.getFailCount();
		if (heartbeatFailCount >= hbState.getMaxFailCount()) {
			connection.close();
			log.error("close connection after heartbeat fail {} times. remote address:{}", heartbeatFailCount,
					connection.remoteAddress());
			return;
		}

		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		RequestMessage heartbeatRequestMessage = protocol.getMessageFactory()
			.createHeartbeatRequest(IDGenerator.nextRequestId(), SerializationType.Hession);

		CallOptions callOptions = CallOptions.builder().timeoutMills(hbState.getTimeoutMills()).build();
		call.asyncCall(heartbeatRequestMessage, connection, callOptions, responseMessage -> {

			HeartbeatState state = HeartbeatState.of(connection);
			if (state == null) {
				return;
			}
			if (responseMessage.getResponseStatus() == ResponseStatus.OK) {
				log.debug("heartbeat success. remote address:{}", connection.remoteAddress());
				state.resetFailCount();
			}
			else {
				int failCount = state.incrementFailCount();
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
