package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageBody;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

/**
 * |type-length:short|value-length:short|type:bytes|value:bytes|
 */
public class RemotingMessageBody implements MessageBody {

    private byte[] bodyData;

    @Setter
    @Getter
    private Object bodyValue;

    private boolean serialized;
    private boolean deserialized;

    public RemotingMessageBody() {
    }

    public RemotingMessageBody(byte[] bodyData) {
        this.bodyData = bodyData;
    }

    @Override
    public void serialize(Serializer serializer) throws SerializeException {
        if (!serialized) {
            serialized = true;

            CompositeByteBuf buf = null;
            try {
                buf = ByteBufAllocator.DEFAULT.compositeBuffer();

                String typeName = bodyValue.getClass().getName();
                byte[] typeData = typeName.getBytes(StandardCharsets.UTF_8);
                byte[] valueData = serializer.serialize(bodyValue);
                buf.writeShort(typeData.length);
                buf.writeShort(valueData.length);
                buf.writeBytes(typeData);
                buf.writeBytes(valueData);

                bodyData = buf.array();
            } finally {
                if (buf != null) {
                    buf.release();
                }
            }
        }
    }

    @Override
    public void deserialize(Serializer serializer) throws DeserializeException {
        if (!deserialized){
            deserialized = true;
            // todo @joecqupt
        }
    }

    @Override
    public byte[] data() {
        return bodyData;
    }

}
