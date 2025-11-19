package io.github.xinfra.lab.remoting.message;

public interface MessageType {

	byte data();

	MessageType heartbeatRequest = () -> (byte) -1;

	MessageType request = () -> (byte) 0;

	MessageType response = () -> (byte) 1;

	static MessageType valueOf(byte data) {
		switch (data) {
			case -1:
				return heartbeatRequest;
			case 0:
				return request;
			case 1:
				return response;
			default:
				throw new IllegalArgumentException("Unknown message type: " + data);
		}
	}

}
