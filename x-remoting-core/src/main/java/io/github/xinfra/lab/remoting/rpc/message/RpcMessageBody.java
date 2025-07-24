package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageBody;
import io.github.xinfra.lab.remoting.serialization.Serializer;

public class RpcMessageBody implements MessageBody {
    @Override
    public byte[] serialize(Serializer serializer) throws SerializeException {

        return null;
    }

    @Override
    public void deserialize(Serializer serializer, byte[] bodyData) throws DeserializeException {

    }
}
