package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.rpc.heartbeat.RpcHeartbeatTrigger;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.rpc.codec.RpcMessageDecoder;
import io.github.xinfra.lab.remoting.rpc.codec.RpcMessageEncoder;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


/**
 * x-rpc protocol definition:
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


    public static final byte[] PROTOCOL_CODE = "x-rpc".getBytes(StandardCharsets.UTF_8);

    private final RpcMessageEncoder rpcMessageEncoder;
    private final RpcMessageDecoder rpcMessageDecoder;
    private final RpcMessageHandler rpcMessageHandler;
    private final RpcMessageFactory rpcMessageFactory;
    private final RpcHeartbeatTrigger rpcHeartbeatTrigger;

    public RpcProtocol() {
        this.rpcMessageFactory = new RpcMessageFactory();
        this.rpcMessageEncoder = new RpcMessageEncoder();
        this.rpcMessageDecoder = new RpcMessageDecoder();
        this.rpcMessageHandler = new RpcMessageHandler();
        this.rpcHeartbeatTrigger = new RpcHeartbeatTrigger();
    }

    @Override
    public byte[] protocolCode() {
        return PROTOCOL_CODE;
    }

    @Override
    public RpcMessageEncoder encoder() {
        return this.rpcMessageEncoder;
    }

    @Override
    public RpcMessageDecoder decoder() {
        return this.rpcMessageDecoder;
    }

    @Override
    public RpcMessageHandler messageHandler() {
        return this.rpcMessageHandler;
    }

    @Override
    public RpcMessageFactory messageFactory() {
        return this.rpcMessageFactory;
    }

    @Override
    public RpcHeartbeatTrigger heartbeatTrigger() {
        return rpcHeartbeatTrigger;
    }

    @Override
    public void close() throws IOException {
        rpcMessageHandler.close();
    }
}
