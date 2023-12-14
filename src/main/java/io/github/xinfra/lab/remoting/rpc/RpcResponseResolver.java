package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SendMessageException;
import io.github.xinfra.lab.remoting.exception.TimeoutException;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;

import java.net.SocketAddress;
import java.util.Objects;


public class RpcResponseResolver {
    public static <R> R getResponseObject(RpcResponseMessage responseMessage, SocketAddress remoteAddress) throws RemotingException {
        ResponseStatus status = ResponseStatus.valueOf(responseMessage.getStatus());

        if (Objects.equals(status, ResponseStatus.SUCCESS)) {
            return (R) responseMessage.getContent();
        }

        switch (status) {
            case TIMEOUT ->
                    throw new TimeoutException("rpc invoke timeout. remote address:" + responseMessage.getRemoteAddress());
            case CLIENT_SEND_ERROR ->
                    throw new SendMessageException("rpc send message fail. remote address:" + responseMessage.getRemoteAddress(),
                            responseMessage.getCause());
            // TODO
        }

        if (responseMessage.getCause() != null) {
            throw new RemotingException("rpc invoke fail. remote address:" + remoteAddress,
                    responseMessage.getCause());
        } else {
            throw new RemotingException("rpc invoke fail. remote address:" + remoteAddress);
        }

    }
}
