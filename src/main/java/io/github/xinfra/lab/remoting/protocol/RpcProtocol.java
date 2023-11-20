package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.Decoder;
import io.github.xinfra.lab.remoting.codec.Encoder;
import io.github.xinfra.lab.remoting.message.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;

/**
 * x-protocol
 * <p>
 * request definition:
 * <p>
 * ｜protocol:bytes|message-type:byte|request-id:int|serialization-type:byte|content-type-length:short|header-length:short|content-length:int|content-type|header|content|
 * <p>
 * response definition:
 * <p>
 * ｜protocol:bytes|message-type:byte|request-id:int|serialization-type:byte|status:short|content-type-length:short|header-length:short]content-length:int|content-type|header|content|
 */
public class RpcProtocol implements Protocol {

    private RpcMessageFactory rpcMessageFactory;

    public RpcProtocol() {
        this.rpcMessageFactory = new RpcMessageFactory();
    }

    @Override
    public Encoder encoder() {
        // TODO
        return null;
    }

    @Override
    public Decoder decoder() {
        // TODO
        return null;
    }

    @Override
    public MessageHandler messageHandler() {
        // TODO
        return null;
    }

    @Override
    public RpcMessageFactory messageFactory() {
        return this.rpcMessageFactory;
    }

    @Override
    public HeartbeatTrigger heartbeatTrigger() {
        // TODO
        return null;
    }
}
