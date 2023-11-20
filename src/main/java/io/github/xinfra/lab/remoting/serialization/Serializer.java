package io.github.xinfra.lab.remoting.serialization;

public interface Serializer<T> {

    byte[] serialize(T t);


    T deserialize(byte[] data);
}
