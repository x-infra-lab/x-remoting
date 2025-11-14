package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.ConnectionConfig;
import io.github.xinfra.lab.remoting.connection.ConnectionManagerConfig;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandlerRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

@Slf4j
public class RemotingClient extends AbstractLifeCycle {

	private static final Logger log = LoggerFactory.getLogger(RemotingClient.class);

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

		ConnectionConfig connectionConfig = config.getConnectionConfig();
		ConnectionManagerConfig connectionManagerConfig = config.getConnectionManagerConfig();
		if (connectionConfig != null && connectionManagerConfig != null) {
			this.connectionManager = new ClientConnectionManager(protocol, connectionConfig, connectionManagerConfig);
		}
		else if (connectionConfig != null) {
			this.connectionManager = new ClientConnectionManager(protocol, connectionConfig);
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

	public <R> R syncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException, InterruptedException {

		return clientRemotingCall.syncCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> RemotingFuture<R> asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {
		return clientRemotingCall.asyncCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> void asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions,
			RemotingCallBack<R> remotingCallBack) throws RemotingException {
		clientRemotingCall.asyncCall(requestApi, request, socketAddress, callOptions, remotingCallBack);
	}

	public void oneway(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions) throws RemotingException {
		clientRemotingCall.oneway(requestApi, request, socketAddress, CallOptions);
	}



}
