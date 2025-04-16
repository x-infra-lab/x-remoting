package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.util.Objects;

@Slf4j
public class RpcResponses {

	private RpcResponses() {
	}

	public static <R> R getResponseObject(RpcResponseMessage responseMessage) throws RemotingException {
		SocketAddress remoteAddress = responseMessage.getRemoteAddress();
		// todo: fixme classloader problem
		responseMessage.deserialize();
		ResponseStatus status = ResponseStatus.valueOf(responseMessage.getStatus());

		if (Objects.equals(status, ResponseStatus.SUCCESS)) {
			return (R) responseMessage.getContent();
		}

		if (responseMessage.getCause() != null) {
			throw new RemotingException("rpc invoke fail. remote address:" + remoteAddress, responseMessage.getCause());
		}
		else if (responseMessage.getContent() instanceof Throwable) {
			throw new RemotingException("rpc invoke fail. remote address:" + remoteAddress,
					(Throwable) responseMessage.getContent());
		}
		else {
			throw new RemotingException("rpc invoke fail. remote address:" + remoteAddress);
		}

	}

	public static void sendResponse(MessageHandlerContext messageHandlerContext, RpcResponseMessage responseMessage) {
		MessageFactory messageFactory = messageHandlerContext.getMessageFactory();
		int id = responseMessage.id();
		short status = responseMessage.getStatus();
		try {
			responseMessage.serialize();
		}
		catch (SerializeException e) {
			String errorMsg = String.format("sendResponse SerializeException. id: %s", id);
			log.error(errorMsg, e);
			responseMessage = messageFactory.createExceptionResponse(id, e, ResponseStatus.SERVER_SERIAL_EXCEPTION);

			// serialize again
			try {
				responseMessage.serialize();
			}
			catch (SerializeException ex) {
				log.error("serialize SerializeException response fail. id: {}", id, ex);
			}
		}
		catch (Throwable t) {
			String errorMsg = String.format("sendResponse fail. id: %s", id);
			log.error(errorMsg, t);
			responseMessage = messageFactory.createExceptionResponse(id, t, errorMsg);

			// serialize again
			try {
				responseMessage.serialize();
			}
			catch (SerializeException ex) {
				log.error("serialize exception response fail. id: {}", id, ex);
			}
		}

		messageHandlerContext.getChannelContext()
			.writeAndFlush(responseMessage)
			.addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					if (future.isSuccess()) {
						if (log.isInfoEnabled()) {
							log.info("write response success, id={}, status={}", id, status);
						}
					}
					else {
						log.error("write response fail, id={}, status={}", id, status, future.cause());
					}
				}
			});

	}

}
