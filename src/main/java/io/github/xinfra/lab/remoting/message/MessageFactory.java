package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;

import java.net.SocketAddress;

public interface MessageFactory {

	<T extends Message> T createSendFailResponseMessage(int id, Throwable cause, SocketAddress remoteAddress);

	<T extends Message> T createTimeoutResponseMessage(int id, SocketAddress remoteAddress);

	<T extends Message> T createRequestMessage();

	<T extends Message> T createHeartbeatRequestMessage();

	<T extends Message> T createExceptionResponse(int id, Throwable t, ResponseStatus status);

	<T extends Message> T createExceptionResponse(int id, Throwable t, String errorMsg);

	<T extends Message> T createExceptionResponse(int id, String errorMsg);

	<T extends Message> T createResponse(int id, Object responseContent);

	<T extends Message> T createConnectionClosedMessage(int id, SocketAddress remoteAddress);

}
