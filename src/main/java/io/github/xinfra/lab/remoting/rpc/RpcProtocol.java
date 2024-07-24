package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.heartbeat.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.rpc.heartbeat.RpcHeartbeatTrigger;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.rpc.codec.RpcMessageDecoder;
import io.github.xinfra.lab.remoting.rpc.codec.RpcMessageEncoder;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageHandler;

import java.nio.charset.StandardCharsets;

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

    public static int RESPONSE_HEADER_LEN = 21;

    public static int REQUEST_HEADER_LEN = 19;

    public static final ProtocolType RPC = new ProtocolType("x-rpc", "x-rpc".getBytes(StandardCharsets.UTF_8));


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
        return rpcHeartbeatTrigger;
    }
}
