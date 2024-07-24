package io.github.xinfra.lab.remoting.rpc.heartbeat;

import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.rpc.message.RpcRequestMessage;
import io.github.xinfra.lab.remoting.serialization.SerializationType;

public class RpcHeartbeatRequestMessage extends RpcRequestMessage {
    public RpcHeartbeatRequestMessage(int id) {
        super(id, MessageType.heartbeatRequest, SerializationType.HESSION);
    }

    public RpcHeartbeatRequestMessage(int id, SerializationType serializationType) {
        super(id, MessageType.heartbeatRequest, serializationType);
    }
}
