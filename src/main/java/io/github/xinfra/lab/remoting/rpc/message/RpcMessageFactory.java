package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.exception.ConnectionClosedException;
import io.github.xinfra.lab.remoting.exception.SendMessageException;
import io.github.xinfra.lab.remoting.exception.TimeoutException;
import io.github.xinfra.lab.remoting.message.MessageFactory;

import java.net.SocketAddress;


public class RpcMessageFactory implements MessageFactory {
    @Override
    public RpcResponseMessage createSendFailResponseMessage(int id, Throwable cause, SocketAddress remoteAddress) {
        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(id);
        rpcResponseMessage.setStatus(ResponseStatus.CLIENT_SEND_ERROR.getCode());
        rpcResponseMessage.setCause(new SendMessageException(cause));
        rpcResponseMessage.setRemoteAddress(remoteAddress);
        return rpcResponseMessage;
    }

    @Override
    public RpcResponseMessage createTimeoutResponseMessage(int id, SocketAddress remoteAddress) {
        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(id);
        rpcResponseMessage.setStatus(ResponseStatus.TIMEOUT.getCode());
        rpcResponseMessage.setCause(new TimeoutException());
        rpcResponseMessage.setRemoteAddress(remoteAddress);
        return rpcResponseMessage;
    }

    @Override
    public RpcRequestMessage createRequestMessage() {
        return new RpcRequestMessage(IDGenerator.nextRequestId());
    }

    @Override
    public RpcRequestMessage createHeartbeatRequestMessage() {
        return new RpcHeartbeatRequestMessage(IDGenerator.nextRequestId());
    }

    @Override
    public RpcResponseMessage createExceptionResponse(int id, Throwable t, ResponseStatus status) {
        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(id);
        rpcResponseMessage.setStatus(status.getCode());
        rpcResponseMessage.setCause(t);
        return rpcResponseMessage;
    }

    @Override
    public RpcResponseMessage createExceptionResponse(int id, Throwable t, String errorMsg) {
        // todo
        return null;
    }

    @Override
    public RpcResponseMessage createExceptionResponse(int id, String errorMsg) {
        // todo
        return null;
    }

    @Override
    public RpcResponseMessage createExceptionResponse(int id, Throwable t) {
        // todo
        return null;
    }

    @Override
    public RpcResponseMessage createExceptionResponse(int id, ResponseStatus status) {
        // todo
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

    @Override
    public RpcResponseMessage createConnectionClosedMessage(int id, SocketAddress remoteAddress) {
        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(id);
        rpcResponseMessage.setStatus(ResponseStatus.CONNECTION_CLOSED.getCode());
        rpcResponseMessage.setCause(new ConnectionClosedException());
        rpcResponseMessage.setRemoteAddress(remoteAddress);
        return rpcResponseMessage;
    }
}
