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

import java.io.IOException;
import java.net.SocketAddress;

@Slf4j
public class RpcClient extends AbstractLifeCycle {

	private static final Logger log = LoggerFactory.getLogger(RpcClient.class);

	@Getter
	private RpcClientConfig config;

	@Getter
	private RemotingProtocol protocol;

	private RemotingCall rpcClientRemoting;

	private RequestHandlerRegistry requestHandlerRegistry = new RequestHandlerRegistry();

	@Getter
	private ClientConnectionManager connectionManager;

	public RpcClient() {
		this(new RpcClientConfig());
	}

	public RpcClient(RpcClientConfig config) {
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

		this.rpcClientRemoting = new RemotingCall(connectionManager);
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
		try {
			rpcClientRemoting.close();
		}
		catch (IOException e) {
			log.warn("rpcClientRemoting close ex", e);
		}
	}

	public <R> R syncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException, InterruptedException {

		return rpcClientRemoting.syncCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> RpcInvokeFuture<R> asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {
		return rpcClientRemoting.asyncCall(requestApi, request, socketAddress, callOptions);
	}

	public <R> void asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions,
			RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {
		rpcClientRemoting.asyncCall(requestApi, request, socketAddress, callOptions, rpcInvokeCallBack);
	}

	public void oneway(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions) throws RemotingException {
		rpcClientRemoting.oneway(requestApi, request, socketAddress, CallOptions);
	}



}
