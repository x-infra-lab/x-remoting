package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageBody;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import lombok.Getter;
import lombok.Setter;

public class RpcMessageBody implements MessageBody {

    @Getter
    @Setter
    private byte[] bodyData;

    public RpcMessageBody() {
    }

    public RpcMessageBody(byte[] bodyData) {
        this.bodyData = bodyData;
    }

    @Override
    public void serialize(Serializer serializer) throws SerializeException {

    }

    @Override
    public void deserialize(Serializer serializer) throws DeserializeException {

    }

    @Override
    public byte[] data() {
        return bodyData;
    }


}
