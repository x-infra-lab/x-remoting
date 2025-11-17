package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import org.apache.commons.lang3.tuple.Pair;

import java.util.concurrent.ConcurrentHashMap;

/**
 * per message header
 * |serializationType:byte|key-length:short|value-length:short|key:byte[]|value:byte[]
 */
public class DefaultMessageHeaders implements MessageHeaders {

    private ConcurrentHashMap<String, Pair<Key<?>, ?>> headers = new ConcurrentHashMap<>();

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

    }

    @Override
    public void deserialize(Serializer serializer) throws DeserializeException {

    }

    @Override
    public byte[] data() {
        return new byte[0];
    }
}
