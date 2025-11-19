package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocolIdentifier;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

/**
 * request definition:
 * <p>
 * ｜protocol-codes:bytes|protocol-version:byte|message-type:byte|request-id:int|serialization-type:byte|path-length:short|header-length:short|body-length:int|path:bytes|header:bytes|body:bytes|
 */

public class RemotingRequestMessage extends RemotingMessage implements RequestMessage {

    @Setter
    private String path;

    @Setter
    @Getter
    private byte[] pathData;

    private static final Integer protocolCodeLength = RemotingProtocolIdentifier.PROTOCOL_CODE.length;
    private static final Integer protocolVersionLength = Byte.BYTES;
    private static final Integer messageTypeLength = Byte.BYTES;
    private static final Integer requestIdLength = Integer.BYTES;
    private static final Integer serializationTypeLength = Byte.BYTES;
    private static final Integer pathLengthLength = Short.BYTES;
    private static final Integer headerLengthLength = Short.BYTES;
    private static final Integer bodyLengthLength = Integer.BYTES;

    public static final Integer REQUEST_HEADER_BYTES = protocolCodeLength + protocolVersionLength + messageTypeLength + requestIdLength + serializationTypeLength + pathLengthLength + headerLengthLength + bodyLengthLength;

    public RemotingRequestMessage(int id, SerializationType serializationType) {
        super(id, serializationType);
    }

    @Override
    public String path() {
        return this.path;
    }

    @Override
    public void serialize() throws SerializeException {
        super.serialize();
        if (pathData == null) {
            pathData = path.getBytes(StandardCharsets.UTF_8);
        }
    }

    @Override
    public void deserialize() throws DeserializeException {
        super.deserialize();
        if (path == null) {
            path = new String(pathData, StandardCharsets.UTF_8);
        }
    }

}
