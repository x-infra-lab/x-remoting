package io.github.xinfra.lab.remoting.message;

public enum MessageType {

	request, response, onewayRequest, heartbeatRequest,;

	public byte data() {
		return (byte) this.ordinal();
	}

	public static MessageType valueOf(byte data) {
		return MessageType.values()[data];
	}

}
