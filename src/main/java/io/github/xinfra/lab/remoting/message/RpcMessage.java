package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.serialization.SerializationManager;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

public abstract class RpcMessage implements Message {
    private int id;

    private MessageType messageType;

    private ProtocolType protocolType;

    private SerializationType serializationType;

    @Setter
    @Getter
    private String contentType;

    @Setter
    @Getter
    private String header;

    @Setter
    @Getter
    private Object content;

    @Getter
    private byte[] contentTypeData;

    @Getter
    private short contentTypeLength;

    @Getter
    private byte[] headerData;

    @Getter
    private short headerLength;

    @Getter
    private byte[] contentData;

    @Getter
    private int contentLength;

    public RpcMessage(int id, MessageType messageType, SerializationType serializationType) {
        this.id = id;
        this.messageType = messageType;
        this.protocolType = ProtocolType.RPC;
        this.serializationType = serializationType;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public MessageType messageType() {
        return messageType;
    }

    @Override
    public ProtocolType protocolType() {
        return protocolType;
    }

    @Override
    public SerializationType serializationType() {
        return serializationType;
    }

    @Override
    public void serialize() throws SerializeException {
        this.serializeContentType();
        this.serializeHeader();
        this.serializeContent();
    }

    public void serializeContentType() throws SerializeException {
        if (contentType != null) {
            byte[] bytes = contentType.getBytes(StandardCharsets.UTF_8);
            setContentTypeData(bytes);
        }
    }

    public void setContentTypeData(byte[] bytes) {
        if (bytes != null) {
            int length = bytes.length;
            if (length > Short.MAX_VALUE) {
                throw new RuntimeException("contentType length exceed maximum, len=" + length);
            }
            this.contentTypeLength = (short) length;
            this.contentTypeData = bytes;
        }
    }

    public void serializeHeader() throws SerializeException {
        // TODO
    }

    public void setHeaderData(byte[] bytes) {
        if (bytes != null) {
            int length = bytes.length;
            if (length > Short.MAX_VALUE) {
                throw new RuntimeException("contentType length exceed maximum, len=" + length);
            }
            this.headerLength = (short) length;
            this.headerData = bytes;
        }
    }

    public void serializeContent() throws SerializeException {
        if (content != null) {
            Serializer serializer = SerializationManager.getSerializer(serializationType);
            byte[] bytes = serializer.serialize(content);
            setContentData(bytes);
        }
    }

    public void setContentData(byte[] bytes) {
        this.contentLength = bytes.length;
        this.contentData = bytes;
    }

    @Override
    public void deserialize() throws DeserializeException {
        deserializeContentType();
        deserializeHeader();
        deserializeContent();
    }

    public void deserialize(RpcDeserializeLevel level) throws DeserializeException {
        if (level.ordinal() == RpcDeserializeLevel.content_type.ordinal()) {
            deserializeContentType();
        } else if (level.ordinal() == RpcDeserializeLevel.header.ordinal()) {
            deserializeContentType();
            deserializeHeader();
        } else {
            deserialize();
        }
    }

    private void deserializeContent() throws DeserializeException {
        if (content == null && contentData != null) {
            this.content = SerializationManager.getSerializer(serializationType).deserialize(contentData, contentType);
        }
    }

    private void deserializeHeader() {
        if (header == null && headerData != null) {
            // TODO
        }
    }

    private void deserializeContentType() {
        if (contentType == null && contentTypeData != null) {
            this.contentType = new String(contentTypeData, StandardCharsets.UTF_8);
        }
    }


}
