package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.serialization.SerializationType;

public class RpcHeartbeatResponseMessage extends RpcMessage {
    public RpcHeartbeatResponseMessage(int id) {
        super(id, MessageType.heartbeatResponse, SerializationType.HESSION);
    }

    public RpcHeartbeatResponseMessage(int id, SerializationType serializationType) {
        super(id, MessageType.heartbeatResponse, serializationType);
    }
}
