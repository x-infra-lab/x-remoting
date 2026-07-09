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
import io.github.xinfra.lab.remoting.rpc.handler.RequestApi;
import io.github.xinfra.lab.remoting.rpc.handler.RequestHandler;
import io.github.xinfra.lab.remoting.rpc.handler.RequestHandlerRegistry;
import io.github.xinfra.lab.remoting.rpc.heartbeat.DefaultHeartbeater;
import io.github.xinfra.lab.remoting.rpc.heartbeat.ProtocolHeartBeatHandler;
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

	private RequestHandlerRegistry requestHandlerRegistry = new RequestHandlerRegistry();

	@Getter
	private ClientConnectionManager connectionManager;

	public RemotingClient() {
		this(new RemotingClientConfig());
	}

	public RemotingClient(RemotingClientConfig config) {
		this.config = config;
		this.protocol = new RemotingProtocol(requestHandlerRegistry);

		ConnectionFactoryConfig connectionFactoryConfig = config.getConnectionFactoryConfig() != null
				? config.getConnectionFactoryConfig() : ConnectionFactoryConfig.defaults();
		ConnectionManagerConfig connectionManagerConfig = config.getConnectionManagerConfig() != null
				? config.getConnectionManagerConfig() : ConnectionManagerConfig.defaults();
		ReconnectConfig reconnectConfig = config.getReconnectConfig() != null ? config.getReconnectConfig()
				: ReconnectConfig.defaults();

		List<Supplier<ChannelHandler>> channelHandlerSuppliers = buildChannelHandlerSuppliers();

		this.connectionManager = new ClientConnectionManager(protocol, channelHandlerSuppliers, connectionFactoryConfig,
				connectionManagerConfig, reconnectConfig);
		this.clientRemotingCall = new RemotingCall(connectionManager);
	}

	private List<Supplier<ChannelHandler>> buildChannelHandlerSuppliers() {
		ProtocolHandler protocolHandler = new ProtocolHandler();
		ProtocolHeartBeatHandler heartBeatHandler = new ProtocolHeartBeatHandler(new DefaultHeartbeater());
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
	}

	public <R> R blockingCall(RequestApi requestApi, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions) throws RemotingException, InterruptedException {

		return clientRemotingCall.blockingCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> RemotingFuture<R> futureCall(RequestApi requestApi, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions) throws RemotingException {
		return clientRemotingCall.futureCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> void asyncCall(RequestApi requestApi, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions, RemotingCallBack<R> remotingCallBack) throws RemotingException {
		clientRemotingCall.asyncCall(requestApi, request, socketAddress, callOptions, remotingCallBack);
	}

	public void oneway(RequestApi requestApi, Object request, InetSocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {
		clientRemotingCall.oneway(requestApi, request, socketAddress, callOptions);
	}

	public <T, R> void registerRequestHandler(RequestApi requestApi, RequestHandler<T, R> userProcessor) {
		requestHandlerRegistry.register(requestApi, userProcessor);
	}

}
