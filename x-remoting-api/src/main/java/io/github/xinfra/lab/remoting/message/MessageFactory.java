package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;

import java.net.SocketAddress;

public interface MessageFactory {

	<T extends ResponseMessage> T createSendFailResponseMessage(int id, Throwable cause, SocketAddress remoteAddress);

	<T extends ResponseMessage> T createTimeoutResponseMessage(int id, SocketAddress remoteAddress);

	<T extends RequestMessage> T createRequestMessage();

	<T extends RequestMessage> T createHeartbeatRequestMessage();

	<T extends ResponseMessage> T createExceptionResponse(int id, Throwable t, ResponseStatus status);

	<T extends ResponseMessage> T createExceptionResponse(int id, Throwable t, String errorMsg);

	<T extends ResponseMessage> T createExceptionResponse(int id, String errorMsg);

	<T extends ResponseMessage> T createResponse(int id, Object responseContent);

	<T extends ResponseMessage> T createConnectionClosedMessage(int id, SocketAddress remoteAddress);

}
