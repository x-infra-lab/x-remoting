package io.github.xinfra.lab.remoting.message;


import java.net.SocketAddress;

public interface MessageFactory {


    Message createSendFailResponseMessage(int id, Throwable cause, SocketAddress remoteAddress);

    Message createTimeoutResponseMessage(int id, SocketAddress remoteAddress);

    Message createRequestMessage();

    Message createHeartbeatRequestMessage();

    Message createExceptionResponse(int id, Throwable t, ResponseStatus status);

    Message createExceptionResponse(int id, Throwable t, String errorMsg);

    Message createExceptionResponse(int id, String errorMsg);

    Message createExceptionResponse(int id, Throwable t);

    Message createExceptionResponse(int id, ResponseStatus status);

    Message createResponse(int id, Object responseContent);

    Message createConnectionClosedMessage(int id, SocketAddress remoteAddress);
}
