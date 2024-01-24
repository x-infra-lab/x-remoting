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

    @Override
    public Message createHeartbeatRequestMessage() {
        return new RpcRequestMessage(IDGenerator.nextRequestId());
    }

    @Override
    public RpcResponseMessage createExceptionResponse(int id, Throwable t, ResponseStatus status) {
        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(id);
        rpcResponseMessage.setStatus(status.getCode());

        // TODO to be RpcServerException

        rpcResponseMessage.setContent(t);
        rpcResponseMessage.setContentType(t.getClass().getName());
        // TODO
        return null;
    }

    @Override
    public RpcResponseMessage createExceptionResponse(int id, Throwable t, String errorMsg) {
        return null;
    }

    @Override
    public RpcResponseMessage createExceptionResponse(int id, String errorMsg) {
        return null;
    }

    @Override
    public RpcResponseMessage createExceptionResponse(int id, Throwable t) {
        return null;
    }

    @Override
    public RpcResponseMessage createExceptionResponse(int id, ResponseStatus status) {
        return null;
    }

    @Override
    public RpcResponseMessage createResponse(int id, Object responseContent) {
        RpcResponseMessage responseMessage = new RpcResponseMessage(id);
        responseMessage.setStatus(ResponseStatus.SUCCESS.getCode());
        if (responseContent != null) {
            responseMessage.setContent(responseContent);
            responseMessage.setContentType(responseContent.getClass().getName());
        }
        return responseMessage;
    }
}
