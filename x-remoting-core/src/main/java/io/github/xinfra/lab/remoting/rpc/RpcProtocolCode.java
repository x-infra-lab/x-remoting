package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.protocol.ProtocolCode;

import java.nio.charset.StandardCharsets;

public enum RpcProtocolCode implements ProtocolCode {

    INSTANCE;

    public static final byte[] PROTOCOL_CODE = "x-rpc".getBytes(StandardCharsets.UTF_8);

    @Override
    public byte[] code() {
        return PROTOCOL_CODE;
    }
}
