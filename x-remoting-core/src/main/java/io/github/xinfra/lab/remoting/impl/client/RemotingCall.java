package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.client.Call;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.impl.RequestApi;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponses;

import java.net.SocketAddress;

public class RemotingCall implements Call {

	protected ConnectionManager connectionManager;

	public RemotingCall(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public <R> R syncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, int timeoutMills)
			throws InterruptedException, RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);
		MessageFactory messageFactory = connection.getProtocol().messageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, request);

		RemotingResponseMessage responseMessage = (RemotingResponseMessage) syncCall(requestMessage, connection,
				timeoutMills);
		return RemotingResponses.getResponseObject(responseMessage);
	}

	public <R> RpcInvokeFuture<R> asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, int timeoutMills)
			throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		MessageFactory messageFactory = connection.getProtocol().messageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, request);

		InvokeFuture<?> invokeFuture = super.asyncCall(requestMessage, connection, timeoutMills);
		return new RpcInvokeFuture<R>(invokeFuture);
	}

	public <R> void asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, int timeoutMills,
			RpcInvokeCallBack<R> rpcInvokeCallBack) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		MessageFactory messageFactory = connection.getProtocol().messageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, request);

		super.asyncCall(requestMessage, connection, timeoutMills, rpcInvokeCallBack);
	}

	public void oneway(RequestApi requestApi, Object request, SocketAddress socketAddress) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		MessageFactory messageFactory = connection.getProtocol().messageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, request);

		super.oneway(requestMessage, connection);
	}

	private RequestMessage buildRequestMessage(MessageFactory messageFactory, Object request)
			throws SerializeException {
		RemotingRequestMessage requestMessage = messageFactory.createRequestMessage();
		// todo
		requestMessage.setContent(request);
		requestMessage.setContentType(request.getClass().getName());

		requestMessage.serialize();
		return requestMessage;
	}

}
