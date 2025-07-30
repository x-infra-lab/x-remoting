package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.connection.ClientConnectionManager;
import io.github.xinfra.lab.remoting.connection.ConnectionConfig;
import io.github.xinfra.lab.remoting.connection.ConnectionManagerConfig;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
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

	@Getter
	private ClientConnectionManager connectionManager;

	public RpcClient() {
		this(new RpcClientConfig());
	}

	public RpcClient(RpcClientConfig config) {
		this.config = config;
		this.protocol = new RemotingProtocol();

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
