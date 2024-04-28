package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SendMessageException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.exception.TimeoutException;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.RpcMessage;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Objects;


@Slf4j
public class RpcResponses {
    public static <R> R getResponseObject(RpcResponseMessage responseMessage, SocketAddress remoteAddress) throws RemotingException {
        responseMessage.deserialize();
        ResponseStatus status = ResponseStatus.valueOf(responseMessage.getStatus());

        if (Objects.equals(status, ResponseStatus.SUCCESS)) {
            return (R) responseMessage.getContent();
        }

        switch (status) {
            case TIMEOUT:
                throw new TimeoutException("rpc invoke timeout. remote address:" + remoteAddress);
            case CLIENT_SEND_ERROR:
                throw new SendMessageException("rpc send message fail. remote address:" + remoteAddress,
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


    public static void sendResponse(RemotingContext remotingContext,
                                    RpcResponseMessage responseMessage,
                                    RpcMessageFactory rpcMessageFactory) {
        int id = responseMessage.id();
        short status = responseMessage.getStatus();
        try {
            responseMessage.serialize();
        } catch (SerializeException e) {
            String errorMsg = String.format("sendResponse SerializeException. id: %s", id);
            log.error(errorMsg, e);
            responseMessage = rpcMessageFactory.createExceptionResponse(id,
                    e, ResponseStatus.SERVER_SERIAL_EXCEPTION);

            // serialize again
            try {
                responseMessage.serialize();
            } catch (SerializeException ex) {
                log.error("serialize SerializeException response fail. id: {}", id, ex);
            }
        } catch (Throwable t) {
            String errorMsg = String.format("sendResponse fail. id: %s", id);
            log.error(errorMsg, t);
            responseMessage = rpcMessageFactory.createExceptionResponse(id, t, errorMsg);

            // serialize again
            try {
                responseMessage.serialize();
            } catch (SerializeException ex) {
                log.error("serialize exception response fail. id: {}", id, ex);
            }
        }

        remotingContext.getChannelContext().writeAndFlush(responseMessage).addListener(
                new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            if (log.isInfoEnabled()) {
                                log.info("write response success, id={}, status={}",
                                        id, status);
                            }
                        } else {
                            log.error("write response fail, id={}, status={}",
                                    id, status, future.cause());
                        }
                    }
                }
        );

    }
}
