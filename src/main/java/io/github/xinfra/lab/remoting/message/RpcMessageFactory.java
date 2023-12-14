package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.rpc.exception.RpcServerException;

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


    @Override
    public RpcResponseMessage createExceptionResponse(int id, ResponseStatus status, Throwable t) {
        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(id);
        rpcResponseMessage.setStatus(status.getCode());

        // TODO to be RpcServerException

        rpcResponseMessage.setContent(t);
        rpcResponseMessage.setContentType(t.getClass().getName());
        // TODO
        return null;
    }
}
