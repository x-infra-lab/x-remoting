package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.CompositeByteBuf;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * per message header
 * |serializationType:byte|key-length:short|value-length:short|key:byte[]|value:byte[]
 */
public class DefaultMessageHeaders implements MessageHeaders {

    private ConcurrentHashMap<String, Pair<Key<?>, ?>> headers = new ConcurrentHashMap<>();

    byte[] headerData;

    boolean serialized;

    boolean deserialized;

    @Override
    public <T> void put(Key<T> key, T value) {
        headers.put(key.getName(), Pair.of(key, value));
    }

    @Override
    public <T> T get(Key<T> key) {
        Pair<Key<?>, ?> pair = headers.get(key.getName());
        return pair == null ? null : (T) pair.getRight();
    }

    @Override
    public boolean contains(Key<?> key) {
        return headers.containsKey(key.getName());
    }

    @Override
    public void serialize(Serializer serializer) throws SerializeException {
        if (!serialized) {
            serialized = true;
            if (headers.isEmpty()) {
                return;
            }
            CompositeByteBuf buf = ByteBufAllocator.DEFAULT.compositeBuffer();
            for (Pair<Key<?>, ?> pair : headers.values()) {
                Key<?> key = pair.getLeft();
                Object value = pair.getRight();

                buf.writeByte(serializer.serializationType().data());
                byte[] keyData = key.getName().getBytes(StandardCharsets.UTF_8);
                byte[] valueData = serializer.serialize(value);
                buf.writeShort(keyData.length);
                buf.writeShort(valueData.length);
                buf.writeBytes(keyData);
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
            if (headerData == null){
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
