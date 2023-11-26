package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SendMessageException;
import io.github.xinfra.lab.remoting.exception.TimeoutException;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.message.RpcStatusCode;

import java.net.SocketAddress;
import java.util.Objects;

import static io.github.xinfra.lab.remoting.message.RpcStatusCode.CLIENT_SEND_ERROR;
import static io.github.xinfra.lab.remoting.message.RpcStatusCode.TIMEOUT;

public class RpcResponseResolver {
    public static <R> R getResponseObject(RpcResponseMessage responseMessage, SocketAddress remoteAddress) throws RemotingException {

        if (Objects.equals(responseMessage.getStatus(), RpcStatusCode.SUCCESS)) {
            return (R) responseMessage.getContent();
        }

        int status = responseMessage.getStatus();

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
