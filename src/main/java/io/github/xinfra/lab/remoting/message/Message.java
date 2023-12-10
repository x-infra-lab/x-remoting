package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.serialization.SerializationType;

public interface Message {

    int id();

    MessageType messageType();

    ProtocolType protocolType();

    SerializationType serializationType();

    void serialize() throws SerializeException;

}
