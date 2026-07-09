package io.github.xinfra.lab.remoting.rpc.message;

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
		remotingMessageBody.setBodyValue(t);
		remotingResponseMessage.setBody(remotingMessageBody);
		return remotingResponseMessage;
	}

}
