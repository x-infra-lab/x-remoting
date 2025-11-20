package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Objects;

@Slf4j
public class RemotingResponses {

    private RemotingResponses() {
    }

    public static <R> R getResponseObject(RemotingResponseMessage remotingResponseMessage)
            throws RemotingException {
        // todo: fix classloader problem
        remotingResponseMessage.deserialize();
        ResponseStatus responseStatus = remotingResponseMessage.responseStatus();
        RemotingMessageBody body = remotingResponseMessage.body();
        if (Objects.equals(responseStatus, ResponseStatus.OK)) {
            return body == null ? null : (R) body.getBodyValue();
        }

        // todo @joecqupt
        if (body.getBodyValue() instanceof Throwable) {

            throw new RemotingException("rpc invoke fail. ", (Throwable) body.getBodyValue());
        } else {
            throw new RemotingException("rpc invoke fail. " + body.getBodyValue());
        }
    }

//    public static void sendResponse(MessageHandlerContext messageHandlerContext,
//                                    RemotingResponseMessage rpcResponseMessage) {
//        MessageFactory messageFactory = messageHandlerContext.getMessageFactory();
//        int id = rpcResponseMessage.id();
//        short status = rpcResponseMessage.getStatus();
//        try {
//            rpcResponseMessage.serialize();
//        } catch (SerializeException e) {
//            String errorMsg = String.format("sendResponse SerializeException. id: %s", id);
//            log.error(errorMsg, e);
//            rpcResponseMessage = messageFactory.createResponse(id,
//                    rpcResponseMessage.serializationType(), e,
//                    ResponseStatus.SERVER_SERIAL_EXCEPTION);
//
//            // serialize again
//            try {
//                rpcResponseMessage.serialize();
//            } catch (SerializeException ex) {
//                log.error("serialize SerializeException response fail. id: {}", id, ex);
//            }
//        } catch (Throwable t) {
//            String errorMsg = String.format("sendResponse fail. id: %s", id);
//            log.error(errorMsg, t);
//            rpcResponseMessage = messageFactory.createResponse(id,
//                    rpcResponseMessage.serializationType(), t, errorMsg);
//
//            // serialize again
//            try {
//                rpcResponseMessage.serialize();
//            } catch (SerializeException ex) {
//                log.error("serialize exception response fail. id: {}", id, ex);
//            }
//        }
//
//        messageHandlerContext.getChannelContext()
//                .writeAndFlush(rpcResponseMessage)
//                .addListener(new ChannelFutureListener() {
//                    @Override
//                    public void operationComplete(ChannelFuture future) throws Exception {
//                        if (future.isSuccess()) {
//                            if (log.isInfoEnabled()) {
//                                log.info("write response success, id={}, status={}", id, status);
//                            }
//                        } else {
//                            log.error("write response fail, id={}, status={}", id, status, future.cause());
//                        }
//                    }
//                });
//
//    }

}
