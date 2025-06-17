package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.serialization.SerializationType;

public class RpcHeartbeatRequestMessage extends RpcRequestMessage {

	public RpcHeartbeatRequestMessage(int id) {
		super(id, RpcMessageType.heartbeatRequest, SerializationType.HESSION);
	}

	public RpcHeartbeatRequestMessage(int id, SerializationType serializationType) {
		super(id, RpcMessageType.heartbeatRequest, serializationType);
	}

}
