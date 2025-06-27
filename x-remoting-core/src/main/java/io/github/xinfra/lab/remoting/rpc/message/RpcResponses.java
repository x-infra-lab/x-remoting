package io.github.xinfra.lab.remoting.rpc.message;

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

	public static <R> R getResponseObject(RpcResponseMessage rpcResponseMessage) throws RemotingException {
		SocketAddress remoteAddress = rpcResponseMessage.getRemoteAddress();
		// todo: fixme classloader problem
		rpcResponseMessage.deserialize();
		ResponseStatus status = ResponseStatus.valueOf(rpcResponseMessage.getStatus());

		if (Objects.equals(status, ResponseStatus.SUCCESS)) {
			return (R) rpcResponseMessage.getContent();
		}

		if (rpcResponseMessage.getCause() != null) {
			throw new RemotingException("rpc invoke fail. remote address:" + remoteAddress, rpcResponseMessage.getCause());
		}
		else if (rpcResponseMessage.getContent() instanceof Throwable) {
			throw new RemotingException("rpc invoke fail. remote address:" + remoteAddress,
					(Throwable) rpcResponseMessage.getContent());
		}
		else {
			throw new RemotingException("rpc invoke fail. remote address:" + remoteAddress);
		}

	}

	public static void sendResponse(MessageHandlerContext messageHandlerContext, RpcResponseMessage rpcResponseMessage) {
		MessageFactory messageFactory = messageHandlerContext.getMessageFactory();
		int id = rpcResponseMessage.id();
		short status = rpcResponseMessage.getStatus();
		try {
			rpcResponseMessage.serialize();
		}
		catch (SerializeException e) {
			String errorMsg = String.format("sendResponse SerializeException. id: %s", id);
			log.error(errorMsg, e);
			rpcResponseMessage = messageFactory.createResponse(id, e, ResponseStatus.SERVER_SERIAL_EXCEPTION);

			// serialize again
			try {
				rpcResponseMessage.serialize();
			}
			catch (SerializeException ex) {
				log.error("serialize SerializeException response fail. id: {}", id, ex);
			}
		}
		catch (Throwable t) {
			String errorMsg = String.format("sendResponse fail. id: %s", id);
			log.error(errorMsg, t);
			rpcResponseMessage = messageFactory.createResponse(id, t, errorMsg);

			// serialize again
			try {
				rpcResponseMessage.serialize();
			}
			catch (SerializeException ex) {
				log.error("serialize exception response fail. id: {}", id, ex);
			}
		}

		messageHandlerContext.getChannelContext()
			.writeAndFlush(rpcResponseMessage)
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
