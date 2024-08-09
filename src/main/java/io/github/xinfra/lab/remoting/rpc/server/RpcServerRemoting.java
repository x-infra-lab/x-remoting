package io.github.xinfra.lab.remoting.rpc.server;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.connection.ServerConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.github.xinfra.lab.remoting.rpc.client.RpcInvokeCallBack;
import io.github.xinfra.lab.remoting.rpc.client.RpcInvokeFuture;
import io.github.xinfra.lab.remoting.rpc.client.RpcRemoting;

import java.net.SocketAddress;

public class RpcServerRemoting extends RpcRemoting {

	private ConnectionManager connectionManager;

	public RpcServerRemoting(RpcProtocol protocol, ServerConnectionManager connectionManager) {
		super(protocol);
		this.connectionManager = connectionManager;
	}

	public <R> R syncCall(Object request, SocketAddress socketAddress, int timeoutMills)
			throws InterruptedException, RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		if (null == connection) {
			throw new RemotingException("Client address [" + socketAddress + "] not connected yet!");
		}
		connectionManager.check(connection);

		return this.syncCall(request, connection, timeoutMills);
	}

	public <R> RpcInvokeFuture<R> asyncCall(Object request, SocketAddress socketAddress, int timeoutMills)
			throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		if (null == connection) {
			throw new RemotingException("Client address [" + socketAddress + "] not connected yet!");
		}

		connectionManager.check(connection);

		return super.asyncCall(request, connection, timeoutMills);
	}

	public <R> void asyncCall(Object request, SocketAddress socketAddress, int timeoutMills,
			RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		if (null == connection) {
			throw new RemotingException("Client address [" + socketAddress + "] not connected yet!");
		}
		connectionManager.check(connection);

		super.asyncCall(request, connection, timeoutMills, rpcInvokeCallBack);
	}

	public void oneway(Object request, SocketAddress socketAddress) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		if (null == connection) {
			throw new RemotingException("Client address [" + socketAddress + "] not connected yet!");
		}

		connectionManager.check(connection);

		super.oneway(request, connection);
	}

}
