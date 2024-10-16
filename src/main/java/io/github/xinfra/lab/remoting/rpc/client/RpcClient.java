package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.ConnectionConfig;
import io.github.xinfra.lab.remoting.connection.ConnectionManagerConfig;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.SocketAddress;

@Slf4j
public class RpcClient extends AbstractLifeCycle {

	@Getter
	private RpcClientConfig config;

	@Getter
	private RpcProtocol protocol;

	private RpcRemoting rpcClientRemoting;

	@Getter
	private ClientConnectionManager connectionManager;

	public RpcClient(RpcClientConfig config) {
		this.config = config;
		this.protocol = new RpcProtocol();

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

		this.rpcClientRemoting = new RpcRemoting(protocol, connectionManager);
	}

	public RpcClient() {
		this(new RpcClientConfig());
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
			protocol.close();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public <R> R syncCall(Object request, SocketAddress socketAddress, int timeoutMills)
			throws RemotingException, InterruptedException {

		return rpcClientRemoting.syncCall(request, socketAddress, timeoutMills);
	}

	public <R> RpcInvokeFuture<R> asyncCall(Object request, SocketAddress socketAddress, int timeoutMills)
			throws RemotingException {
		return rpcClientRemoting.asyncCall(request, socketAddress, timeoutMills);
	}

	public <R> void asyncCall(Object request, SocketAddress socketAddress, int timeoutMills,
			RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {
		rpcClientRemoting.asyncCall(request, socketAddress, timeoutMills, rpcInvokeCallBack);
	}

	public void oneway(Object request, SocketAddress socketAddress) throws RemotingException {
		rpcClientRemoting.oneway(request, socketAddress);
	}

	public void registerUserProcessor(UserProcessor<?> userProcessor) {
		protocol.messageHandler().registerUserProcessor(userProcessor);
	}

}
