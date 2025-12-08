package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.client.Call;
import io.github.xinfra.lab.remoting.client.CallOptions;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.impl.handler.RequestApi;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageBody;
import io.github.xinfra.lab.remoting.impl.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponses;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.Requests;

import java.net.SocketAddress;

public class RemotingCall implements Call {

	protected ConnectionManager connectionManager;

	public RemotingCall(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public <R> R blockingCall(RequestApi requestApi, Object request, SocketAddress socketAddress,
			CallOptions callOptions) throws InterruptedException, RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);
		MessageFactory messageFactory = connection.getProtocol().getMessageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, requestApi, request, callOptions);

		RemotingResponseMessage responseMessage = (RemotingResponseMessage) blockingCall(requestMessage, connection,
				callOptions);
		return RemotingResponses.getResponseObject(responseMessage);
	}

	public <R> RemotingFuture<R> futureCall(RequestApi requestApi, Object request, SocketAddress socketAddress,
			CallOptions callOptions) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		MessageFactory messageFactory = connection.getProtocol().getMessageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, requestApi, request, callOptions);

		InvokeFuture<?> invokeFuture = futureCall(requestMessage, connection, callOptions);
		return new RemotingFuture<R>(invokeFuture);
	}

	public <R> void asyncCall(RequestApi requestApi, Object request, SocketAddress socketAddress,
			CallOptions callOptions, RemotingCallBack<R> remotingCallBack) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		MessageFactory messageFactory = connection.getProtocol().getMessageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, requestApi, request, callOptions);

		asyncCall(requestMessage, connection, callOptions, remotingCallBack);
	}

	public void oneway(RequestApi requestApi, Object request, SocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		MessageFactory messageFactory = connection.getProtocol().getMessageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, requestApi, request, callOptions);
		Requests.markOnewayRequest(requestMessage);
		oneway(requestMessage, connection, callOptions);
	}

	private RequestMessage buildRequestMessage(MessageFactory messageFactory, RequestApi requestApi, Object request,
			CallOptions callOptions) throws SerializeException {
		RemotingRequestMessage requestMessage = messageFactory.createRequest(IDGenerator.nextRequestId(),
				callOptions.getSerializationType());
		requestMessage.setPath(requestApi.path());
		requestMessage.setHeaders(callOptions.getHeaders());
		requestMessage.setBody(new RemotingMessageBody(request));
		requestMessage.serialize();
		return requestMessage;
	}

}
