package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;

import java.net.SocketAddress;

public class RpcMessageFactory implements MessageFactory {
    @Override
    public RpcResponseMessage createSendFailResponseMessage(SocketAddress remoteAddress, Throwable cause) {
        // TODO
        return null;
    }

    @Override
    public RpcResponseMessage createTimeoutResponseMessage(SocketAddress remoteAddress) {
        // TODO
        return null;
    }

    @Override
    public RpcRequestMessage createRequestMessage() {
        return new RpcRequestMessage(IDGenerator.nextRequestId());
    }
}
