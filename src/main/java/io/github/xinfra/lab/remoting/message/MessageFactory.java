package io.github.xinfra.lab.remoting.message;



public interface MessageFactory {


    Message createSendFailResponseMessage(int id, Throwable cause);

    Message createTimeoutResponseMessage(int id);

    Message createRequestMessage();

    Message createHeartbeatRequestMessage();

    Message createExceptionResponse(int id, Throwable t, ResponseStatus status);

    Message createExceptionResponse(int id, Throwable t, String errorMsg);

    Message createExceptionResponse(int id, String errorMsg);

    Message createExceptionResponse(int id, Throwable t);

    Message createExceptionResponse(int id, ResponseStatus status);

    Message createResponse(int id, Object responseContent);
}
