package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.message.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.rpc.RpcHeartbeatTrigger;
import io.github.xinfra.lab.remoting.rpc.RpcMessageDecoder;
import io.github.xinfra.lab.remoting.rpc.RpcMessageEncoder;
import io.github.xinfra.lab.remoting.rpc.RpcMessageHandler;

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

    private MessageEncoder rpcMessageEncoder;
    private MessageDecoder rpcMessageDecoder;
    private MessageHandler rpcMessageHandler;
    private RpcMessageFactory rpcMessageFactory;
    private HeartbeatTrigger rpcHeartbeatTrigger;

    public RpcProtocol() {
        this.rpcMessageFactory = new RpcMessageFactory();
        this.rpcMessageEncoder = new RpcMessageEncoder();
        this.rpcMessageDecoder = new RpcMessageDecoder();
        this.rpcMessageHandler = new RpcMessageHandler(rpcMessageFactory);
        this.rpcHeartbeatTrigger = new RpcHeartbeatTrigger(rpcMessageFactory);
    }

    @Override
    public MessageEncoder encoder() {
        return this.rpcMessageEncoder;
    }

    @Override
    public MessageDecoder decoder() {
        return this.rpcMessageDecoder;
    }

    @Override
    public MessageHandler messageHandler() {
        return this.rpcMessageHandler;
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
