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

    private RpcMessageHeader header;

    @Getter
    @Setter
    private byte[] headerData;

    private RpcMessageBody body;

    @Getter
    @Setter
    private byte[] bodyData;

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
        if (headerData == null) {
            if (header != null) {
                headerData = header.serialize(serializer);
            }
        }
        if (bodyData == null) {
            if (body != null) {
                bodyData = body.serialize(serializer);
            }
        }
    }

    @Override
    public void deserialize() throws DeserializeException {
        Serializer serializer = SerializationManager.getSerializer(serializationType);
        if (header == null) {
            if (headerData != null) {
                header = new RpcMessageHeader();
                header.deserialize(serializer, headerData);
            }
        }

        if (body == null) {
            if (bodyData != null) {
                body = new RpcMessageBody();
                body.deserialize(serializer, bodyData);
            }
        }

    }
}
