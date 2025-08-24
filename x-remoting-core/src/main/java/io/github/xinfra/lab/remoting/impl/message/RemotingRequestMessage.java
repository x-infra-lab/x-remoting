package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.Getter;
import lombok.Setter;
import java.nio.charset.StandardCharsets;

/**
 * request definition:
 * <p>
 * ｜protocol:bytes|protocol-version:byte|message-type:byte|request-id:int|serialization-type:byte|path-length:short|header-length:short|body-length:int|path|header|content|
 */


public class RemotingRequestMessage extends RemotingMessage implements RequestMessage {

    @Setter
    private String path;

    @Setter
    @Getter
    private byte[] pathData;

    public RemotingRequestMessage(int id, MessageType messageType, SerializationType serializationType) {
        super(id, messageType, serializationType);
    }

    @Override
    public String path() {
        return this.path;
    }

    @Override
    public void serialize() throws SerializeException {
        super.serialize();
        if(path == null){
            return;
        }
        if (pathData == null) {
            pathData = path.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public void deserialize() throws DeserializeException {
        super.deserialize();
        if (pathData == null){
            return;
        }
        if (path == null) {
            path = new String(pathData, StandardCharsets.UTF_8);
        }
    }
}
