package io.github.xinfra.lab.remoting.rpc.heartbeat;

import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.rpc.client.Call;
import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.rpc.client.IDGenerator;
import io.github.xinfra.lab.remoting.rpc.client.InvokeCallBack;
import io.github.xinfra.lab.remoting.rpc.message.RequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.ResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.rpc.protocol.RpcProtocol;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class DefaultHeartbeater implements Heartbeater {

	private final Set<Connection> disabledConnections = ConcurrentHashMap.newKeySet();

	private final Set<InetSocketAddress> disabledSocketAddresses = ConcurrentHashMap.newKeySet();

	private final Call call;

	private final IDGenerator idGenerator;

	private final ExecutorService heartbeatExecutor;

	public DefaultHeartbeater(IDGenerator idGenerator) {
		this.idGenerator = idGenerator;
		this.call = new Call() {
		};
		this.heartbeatExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("Heartbeat-Callback", true));
	}

	@Override
	public void triggerHeartBeat(Connection connection) {
		if (disabledConnections.contains(connection)) {
			log.debug("heartbeat is disabled. connection:{}", connection);
			return;
		}
		if (disabledSocketAddresses.contains(connection.inetRemoteAddress())) {
			log.debug("heartbeat is disabled for socket address:{}", connection.remoteAddress());
			return;
		}
		if (!connection.getChannel().isWritable()) {
			log.debug("skip heartbeat, channel not writable. remote address:{}", connection.remoteAddress());
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
			.createHeartbeatRequest(idGenerator.nextRequestId(), SerializationType.Hessian);

		CallOptions callOptions = CallOptions.builder().timeoutMills(hbState.getTimeoutMills()).build();
		call.asyncCall(heartbeatRequestMessage, connection, callOptions, new InvokeCallBack() {
			@Override
			public void onMessage(ResponseMessage responseMessage) {
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
			}

			@Override
			public Executor getExecutor() {
				return heartbeatExecutor;
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

	@Override
	public void shutdown() {
		heartbeatExecutor.shutdown();
	}

}
