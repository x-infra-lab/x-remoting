package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionManager;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.rpc.handler.RequestApi;
import io.github.xinfra.lab.remoting.rpc.message.MessageFactory;
import io.github.xinfra.lab.remoting.rpc.message.RemotingMessageBody;
import io.github.xinfra.lab.remoting.rpc.message.RemotingRequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.RemotingResponses;
import io.github.xinfra.lab.remoting.rpc.message.RequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.Requests;
import io.github.xinfra.lab.remoting.rpc.protocol.RpcProtocol;

import java.net.InetSocketAddress;

public class RemotingCall implements Call {

	protected ConnectionManager connectionManager;

	public RemotingCall(ConnectionManager connectionManager) {
		this.connectionManager = connectionManager;
	}

	public <R> R blockingCall(RequestApi requestApi, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions) throws InterruptedException, RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);
		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		MessageFactory messageFactory = protocol.getMessageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, requestApi, request, callOptions);

		RemotingResponseMessage responseMessage = (RemotingResponseMessage) blockingCall(requestMessage, connection,
				callOptions);
		return RemotingResponses.getResponseObject(responseMessage);
	}

	public <R> RemotingFuture<R> futureCall(RequestApi requestApi, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		MessageFactory messageFactory = protocol.getMessageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, requestApi, request, callOptions);

		InvokeFuture<?> invokeFuture = futureCall(requestMessage, connection, callOptions);
		return new RemotingFuture<R>(invokeFuture);
	}

	public <R> void asyncCall(RequestApi requestApi, Object request, InetSocketAddress socketAddress,
			CallOptions callOptions, RemotingCallBack<R> remotingCallBack) throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		MessageFactory messageFactory = protocol.getMessageFactory();
		RequestMessage requestMessage = buildRequestMessage(messageFactory, requestApi, request, callOptions);

		asyncCall(requestMessage, connection, callOptions, remotingCallBack);
	}

	public void oneway(RequestApi requestApi, Object request, InetSocketAddress socketAddress, CallOptions callOptions)
			throws RemotingException {

		Connection connection = connectionManager.get(socketAddress);
		connectionManager.check(connection);

		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		MessageFactory messageFactory = protocol.getMessageFactory();
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
