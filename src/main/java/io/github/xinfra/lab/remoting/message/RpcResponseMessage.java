package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.Getter;
import lombok.Setter;

/**
 * response definition:
 * <p>
 * ｜protocol:bytes|message-type:byte|request-id:int|serialization-type:byte|status:short|content-type-length:short|header-length:short]content-length:int|content-type|header|content|
 */

@Setter
@Getter
public class RpcResponseMessage extends RpcMessage {

    @Setter
    @Getter
    private short status;

    public RpcResponseMessage(int id) {
        this(id, SerializationType.HESSION);
    }

    public RpcResponseMessage(int id, SerializationType serializationType) {
        super(id, MessageType.response, serializationType);
    }
}
