package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.message.MessageType;

public enum RpcMessageType implements MessageType {

	request, response, onewayRequest, heartbeatRequest,;

	public byte data() {
		return (byte) this.ordinal();
	}

	public static RpcMessageType valueOf(byte data) {
		return RpcMessageType.values()[data];
	}

}
