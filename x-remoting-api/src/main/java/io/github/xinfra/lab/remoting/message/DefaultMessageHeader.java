package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.serialization.Serializer;

import java.util.concurrent.ConcurrentHashMap;

public class DefaultMessageHeader implements MessageHeader{

    private ConcurrentHashMap<Key<?> , ?> headers = new ConcurrentHashMap<>();

    @Override
    public <T> void put(Key<T> key, T value) {

    }

    @Override
    public <T> T get(Key<T> key) {
        return null;
    }

    @Override
    public boolean contains(Key<?> key) {
        return false;
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
