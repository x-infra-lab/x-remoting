package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.client.Call;
import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
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

	public <R> R syncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
			throws InterruptedException, RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);
		MessageFactory messageFactory = connection.getProtocol().messageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, request);

		RemotingResponseMessage responseMessage = (RemotingResponseMessage) syncCall(requestMessage, connection,
				 callOptions);
		return RemotingResponses.getResponseObject(responseMessage);
	}

	public <R> RemotingInvokeFuture<R> asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		MessageFactory messageFactory = connection.getProtocol().messageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, request);

		InvokeFuture<?> invokeFuture = asyncCall(requestMessage, connection, callOptions);
		return new RemotingInvokeFuture<R>(invokeFuture);
	}

	public <R> void asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions,
			RemotingInvokeCallBack<R> remotingInvokeCallBack) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		MessageFactory messageFactory = connection.getProtocol().messageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, request);

		asyncCall(requestMessage, connection, callOptions, remotingInvokeCallBack);
	}

	public void oneway(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		MessageFactory messageFactory = connection.getProtocol().messageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, request);

		oneway(requestMessage, connection, callOptions);
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
