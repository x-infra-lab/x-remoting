package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.serialization.SerializationType;

public class RemotingMessageFactory implements MessageFactory {

	@Override
	public RemotingRequestMessage createRequest(int id, SerializationType serializationType) {
		RemotingRequestMessage remotingRequestMessage = new RemotingRequestMessage(id, MessageType.request,
				serializationType);
		return remotingRequestMessage;
	}

	@Override
	public RemotingRequestMessage createHeartbeatRequest(int id, SerializationType serializationType) {
		RemotingRequestMessage remotingRequestMessage = new RemotingRequestMessage(id, MessageType.heartbeatRequest,
				serializationType);
		return remotingRequestMessage;
	}

	@Override
	public RemotingResponseMessage createResponse(int id, SerializationType serializationType, ResponseStatus status) {
		RemotingResponseMessage remotingResponseMessage = new RemotingResponseMessage(id, serializationType, status);
		return remotingResponseMessage;
	}

	@Override
	public RemotingResponseMessage createResponse(int id, SerializationType serializationType, ResponseStatus status,
			Throwable t) {
		RemotingResponseMessage remotingResponseMessage = new RemotingResponseMessage(id, serializationType, status);
		RemotingMessageBody remotingMessageBody = new RemotingMessageBody();
		remotingMessageBody.setValue(t);
		remotingResponseMessage.setBody(remotingMessageBody);
		return remotingResponseMessage;
	}

}
