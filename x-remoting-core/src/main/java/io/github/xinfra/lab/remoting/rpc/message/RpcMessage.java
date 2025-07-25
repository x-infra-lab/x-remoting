package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.protocol.ProtocolIdentifier;
import io.github.xinfra.lab.remoting.rpc.RpcProtocolIdentifier;
import io.github.xinfra.lab.remoting.serialization.SerializationManager;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import lombok.Getter;
import lombok.Setter;

public abstract class RpcMessage implements Message {

    private int id;

    private MessageType messageType;

    private SerializationType serializationType;

    private RpcMessageHeader header = new RpcMessageHeader();

    private RpcMessageBody body = new RpcMessageBody();

    public RpcMessage(int id, MessageType messageType, SerializationType serializationType) {
        this.id = id;
        this.messageType = messageType;
        this.serializationType = serializationType;
    }

    @Override
    public ProtocolIdentifier protocolIdentifier() {
        return RpcProtocolIdentifier.INSTANCE;
    }

    @Override
    public int id() {
        return id;
    }

    public SerializationType serializationType() {
        return serializationType;
    }

    @Override
    public MessageType messageType() {
        return messageType;
    }

    @Override
    public RpcMessageHeader header() {
        return header;
    }

    @Override
    public RpcMessageBody body() {
        return body;
    }

    @Override
    public void serialize() throws SerializeException {
        Serializer serializer = SerializationManager.getSerializer(serializationType);
        header.serialize(serializer);
        body.serialize(serializer);
    }

    @Override
    public void deserialize() throws DeserializeException {
        Serializer serializer = SerializationManager.getSerializer(serializationType);
        header.deserialize(serializer);
        body.deserialize(serializer);
    }
}
