package io.github.xinfra.lab.remoting.rpc.message;

public enum RpcMessageType {

	request, response, onewayRequest, heartbeatRequest,;

	public byte data() {
		return (byte) this.ordinal();
	}

	public static RpcMessageType valueOf(byte data) {
		return RpcMessageType.values()[data];
	}

}
