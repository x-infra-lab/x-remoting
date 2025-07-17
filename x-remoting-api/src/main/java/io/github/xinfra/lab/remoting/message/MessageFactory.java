package io.github.xinfra.lab.remoting.message;

public interface MessageFactory {

	<T extends RequestMessage> T createRequestMessage();

	<T extends RequestMessage> T createHeartbeatRequestMessage();

	<T extends ResponseMessage> T createResponse(int id, ResponseStatus status);

	<T extends ResponseMessage> T createResponse(int id, ResponseStatus status, Throwable t);

	<T extends ResponseMessage> T createResponse(int id, ResponseStatus status, Throwable t, String errorMessage);

}
