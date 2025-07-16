package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.client.Remoting;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.rpc.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponses;

import java.net.SocketAddress;

public class RpcRemoting extends Remoting {

	protected ConnectionManager connectionManager;

	public RpcRemoting(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public <R> R syncCall(Object request, SocketAddress socketAddress, int timeoutMills)
			throws InterruptedException, RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		RpcRequestMessage requestMessage = buildRequestMessage(request);

		RpcResponseMessage responseMessage = (RpcResponseMessage) super.syncCall(requestMessage, connection,
				timeoutMills);
		return RpcResponses.getResponseObject(responseMessage);
	}

	public <R> RpcInvokeFuture<R> asyncCall(Object request, SocketAddress socketAddress, int timeoutMills)
			throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		RpcRequestMessage requestMessage = buildRequestMessage(request);

		InvokeFuture<?> invokeFuture = super.asyncCall(requestMessage, connection, timeoutMills);
		return new RpcInvokeFuture<R>(invokeFuture);
	}

	public <R> void asyncCall(Object request, SocketAddress socketAddress, int timeoutMills,
			RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		RpcRequestMessage requestMessage = buildRequestMessage(request);

		super.asyncCall(requestMessage, connection, timeoutMills, rpcInvokeCallBack);
	}

	public void oneway(Object request, SocketAddress socketAddress) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		RpcRequestMessage requestMessage = buildRequestMessage(request);

		super.oneway(requestMessage, connection);
	}

	private RpcRequestMessage buildRequestMessage(Object request) throws SerializeException {
		RpcRequestMessage requestMessage = messageFactory.createRequestMessage();
		requestMessage.setContent(request);
		requestMessage.setContentType(request.getClass().getName());

		requestMessage.serialize();
		return requestMessage;
	}

}
