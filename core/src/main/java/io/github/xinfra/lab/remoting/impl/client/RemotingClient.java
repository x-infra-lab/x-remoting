package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.ConnectionFactoryConfig;
import io.github.xinfra.lab.remoting.connection.ConnectionManagerConfig;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandler;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandlerRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

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

		ConnectionFactoryConfig connectionFactoryConfig = config.getConnectionFactoryConfig();
		ConnectionManagerConfig connectionManagerConfig = config.getConnectionManagerConfig();
		if (connectionFactoryConfig != null && connectionManagerConfig != null) {
			this.connectionManager = new ClientConnectionManager(protocol, connectionFactoryConfig,
					connectionManagerConfig);
		}
		else if (connectionFactoryConfig != null) {
			this.connectionManager = new ClientConnectionManager(protocol, connectionFactoryConfig);
		}
		else if (connectionManagerConfig != null) {
			this.connectionManager = new ClientConnectionManager(protocol, connectionManagerConfig);
		}
		else {
			this.connectionManager = new ClientConnectionManager(protocol);
		}

		this.clientRemotingCall = new RemotingCall(connectionManager);
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

	public <R> R blockingCall(RequestApi requestApi, Object request, SocketAddress socketAddress,
			CallOptions callOptions) throws RemotingException, InterruptedException {

		return clientRemotingCall.blockingCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> RemotingFuture<R> futureCall(RequestApi requestApi, Object request, SocketAddress socketAddress,
			CallOptions callOptions) throws RemotingException {
		return clientRemotingCall.futureCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> void asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress,
			CallOptions callOptions, RemotingCallBack<R> remotingCallBack) throws RemotingException {
		clientRemotingCall.asyncCall(requestApi, request, socketAddress, callOptions, remotingCallBack);
	}

	public void oneway(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {
		clientRemotingCall.oneway(requestApi, request, socketAddress, callOptions);
	}

	public <T, R> void registerRequestHandler(RequestApi requestApi, RequestHandler<T, R> userProcessor) {
		requestHandlerRegistry.register(requestApi, userProcessor);
	}

}
