package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;


public class RpcMessageFactory implements MessageFactory {
    @Override
    public RpcResponseMessage createSendFailResponseMessage(int id, Throwable cause) {
        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(id);
        rpcResponseMessage.setStatus(ResponseStatus.CLIENT_SEND_ERROR.getCode());
        rpcResponseMessage.setCause(cause);
        return rpcResponseMessage;
    }

    @Override
    public RpcResponseMessage createTimeoutResponseMessage(int id) {
        RpcResponseMessage rpcResponseMessage = new RpcResponseMessage(id);
        rpcResponseMessage.setStatus(ResponseStatus.TIMEOUT.getCode());

        return rpcResponseMessage;
    }

    @Override
    public RpcRequestMessage createRequestMessage() {
        return new RpcRequestMessage(IDGenerator.nextRequestId());
    }

    @Override
    public Message createHeartbeatRequestMessage() {
        return new RpcHeartbeatRequestMessage(IDGenerator.nextRequestId());
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
