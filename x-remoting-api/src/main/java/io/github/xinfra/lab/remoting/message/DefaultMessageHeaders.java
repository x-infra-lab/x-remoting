package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * per message header
 * |key-length:short|value-type-length:short|value-length:short|key:byte[]|value-type:byte[]net|value:byte[]
 */
public class DefaultMessageHeaders implements MessageHeaders {

    private ConcurrentHashMap<Key<?>, Object> headers = new ConcurrentHashMap<>();

    byte[] headerData;

    boolean serialized;

    boolean deserialized;

    @Override
    public <T> void put(Key<T> key, T value) {
        headers.put(key, value);
    }

    @Override
    public <T> T get(Key<T> key) {
        return (T) headers.get(key);
    }

    @Override
    public boolean contains(Key<?> key) {
        return headers.contains(key);
    }

    @Override
    public void serialize(Serializer serializer) throws SerializeException {
        if (!serialized) {
            serialized = true;
            if (headers.isEmpty()) {
                return;
            }
            CompositeByteBuf buf = ByteBufAllocator.DEFAULT.compositeBuffer();
            for (Map.Entry<Key<?>, Object> entry : headers.entrySet()) {
                Key<?> key = entry.getKey();
                Object value = entry.getValue();

                byte[] keyData = key.getName().getBytes(StandardCharsets.UTF_8);
                byte[] valueTypeData = key.getType().getName().getBytes(StandardCharsets.UTF_8);
                byte[] valueData = serializer.serialize(value);
                buf.writeShort(keyData.length);
                buf.writeShort(valueTypeData.length);
                buf.writeShort(valueData.length);
                buf.writeBytes(keyData);
                buf.writeBytes(valueTypeData);
                buf.writeBytes(valueData);
            }
            headerData = buf.array();
            buf.release();
        }
    }

    @Override
    public void deserialize(Serializer serializer) throws DeserializeException {
        if (!deserialized) {
            deserialized = true;
            if (headerData == null) {
                return;
            }
            // todo

        }
    }

    @Override
    public byte[] data() {
        return headerData;
    }

}
