package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.ConnectionFactoryConfig;
import io.github.xinfra.lab.remoting.connection.ConnectionManagerConfig;
import io.github.xinfra.lab.remoting.connection.ProtocolDecoder;
import io.github.xinfra.lab.remoting.connection.ProtocolEncoder;
import io.github.xinfra.lab.remoting.connection.ProtocolHandler;
import io.github.xinfra.lab.remoting.connection.ReconnectConfig;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.handler.RequestHandler;
import io.github.xinfra.lab.remoting.rpc.heartbeat.DefaultHeartbeater;
import io.github.xinfra.lab.remoting.rpc.heartbeat.Heartbeater;
import io.github.xinfra.lab.remoting.rpc.heartbeat.ProtocolHeartBeatHandler;
import io.github.xinfra.lab.remoting.rpc.message.RemotingRequestMessageHandler;
import io.github.xinfra.lab.remoting.rpc.protocol.RemotingProtocol;
import io.netty.channel.ChannelHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class RemotingClient extends AbstractLifeCycle {

	@Getter
	private RemotingClientConfig config;

	@Getter
	private RemotingProtocol protocol;

	private RemotingCall clientRemotingCall;

	private RemotingRequestMessageHandler requestMessageHandler = new RemotingRequestMessageHandler();

	private Heartbeater heartbeater;

	@Getter
	private ClientConnectionManager connectionManager;

	public RemotingClient() {
		this(new RemotingClientConfig());
	}

	public RemotingClient(RemotingClientConfig config) {
		this.config = config;
		this.protocol = new RemotingProtocol(requestMessageHandler);

		IDGenerator idGenerator = new IDGenerator();

		ConnectionFactoryConfig connectionFactoryConfig = config.getConnectionFactoryConfig() != null
				? config.getConnectionFactoryConfig() : ConnectionFactoryConfig.defaults();
		ConnectionManagerConfig connectionManagerConfig = config.getConnectionManagerConfig() != null
				? config.getConnectionManagerConfig() : ConnectionManagerConfig.defaults();
		ReconnectConfig reconnectConfig = config.getReconnectConfig() != null ? config.getReconnectConfig()
				: ReconnectConfig.defaults();

		List<Supplier<ChannelHandler>> channelHandlerSuppliers = buildChannelHandlerSuppliers(idGenerator);

		this.connectionManager = new ClientConnectionManager(protocol, channelHandlerSuppliers, connectionFactoryConfig,
				connectionManagerConfig, reconnectConfig);
		this.clientRemotingCall = new RemotingCall(connectionManager, idGenerator);
	}

	private List<Supplier<ChannelHandler>> buildChannelHandlerSuppliers(IDGenerator idGenerator) {
		ProtocolHandler protocolHandler = new ProtocolHandler();
		this.heartbeater = new DefaultHeartbeater(idGenerator);
		ProtocolHeartBeatHandler heartBeatHandler = new ProtocolHeartBeatHandler(this.heartbeater);
		List<Supplier<ChannelHandler>> suppliers = new ArrayList<>();
		suppliers.add(ProtocolEncoder::new);
		suppliers.add(ProtocolDecoder::new);
		suppliers.add(() -> protocolHandler);
		suppliers.add(() -> heartBeatHandler);
		return suppliers;
	}

	@Override
	public void startup() {
		super.startup();
		connectionManager.startup();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		connectionManager.shutdown();
		heartbeater.shutdown();
	}

	public <R> R blockingCall(String path, Object request, InetSocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException, InterruptedException {

		return clientRemotingCall.blockingCall(path, request, socketAddress, callOptions);
	}

	public <R> RemotingFuture<R> futureCall(String path, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions) throws RemotingException {
		return clientRemotingCall.futureCall(path, request, socketAddress, callOptions);
	}

	public <R> void asyncCall(String path, Object request, InetSocketAddress socketAddress, CallOptions callOptions,
			RemotingCallBack<R> remotingCallBack) throws RemotingException {
		clientRemotingCall.asyncCall(path, request, socketAddress, callOptions, remotingCallBack);
	}

	public void oneway(String path, Object request, InetSocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {
		clientRemotingCall.oneway(path, request, socketAddress, callOptions);
	}

	public <T, R> void registerRequestHandler(String path, RequestHandler<T, R> requestHandler) {
		requestMessageHandler.register(path, requestHandler);
	}

}
