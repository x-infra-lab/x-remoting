package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.serialization.SerializationType;

public interface MessageFactory {

	<T extends RequestMessage> T createRequest(int id, SerializationType serializationType);

	<T extends RequestMessage> T createHeartbeatRequest(int id, SerializationType serializationType);

	<T extends ResponseMessage> T createResponse(int id, SerializationType serializationType, ResponseStatus status);

	<T extends ResponseMessage> T createResponse(int id, SerializationType serializationType, ResponseStatus status,
			Throwable t);

}
