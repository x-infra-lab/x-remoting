package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.serialization.SerializationType;

public interface MessageFactory {

	<T extends RequestMessage> T createRequest(int id, MessageType messageType, SerializationType serializationType);

	<T extends ResponseMessage> T createResponse(int id, ResponseStatus status);

	<T extends ResponseMessage> T createResponse(int id, ResponseStatus status, Throwable t);

	<T extends ResponseMessage> T createResponse(int id, ResponseStatus status, Throwable t, String errorMessage);

}
