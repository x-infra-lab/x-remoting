package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.rpc.exception.ResponseStatusRuntimeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.rpc.protocol.RpcProtocol;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Responses {

	private static MessageFactory messageFactory(Connection connection) {
		return ((RpcProtocol) connection.getProtocol()).getMessageFactory();
	}

	public static void handleExceptionResponse(Connection connection, Message msg, Exception e) {
		if (isNeedResponse(msg)) {
			MessageFactory factory = messageFactory(connection);
			if (e instanceof ResponseStatusRuntimeException) {
				ResponseStatusRuntimeException statusException = (ResponseStatusRuntimeException) e;
				ResponseMessage response = factory.createResponse(msg.getId(), msg.getSerializationType(),
						statusException.getResponseStatus());
				Responses.sendResponse(connection, response);
			}
			else {
				ResponseMessage response = factory.createResponse(msg.getId(), msg.getSerializationType(),
						ResponseStatus.Error, e);
				Responses.sendResponse(connection, response);
			}
		}
	}

	public static boolean isNeedResponse(Message msg) {
		return msg instanceof RequestMessage && !Requests.isOnewayRequest((RequestMessage) msg);
	}

	public static void sendResponse(Connection connection, ResponseMessage responseMessage) {
		try {
			responseMessage.serialize();
		}
		catch (Throwable t) {
			log.error("responseMessage serialize fail.", t);

			ResponseStatus status = t instanceof SerializeException ? ResponseStatus.SerializeException
					: ResponseStatus.Error;

			responseMessage = messageFactory(connection).createResponse(responseMessage.getId(),
					responseMessage.getSerializationType(), status, t);
			try {
				responseMessage.serialize();
			}
			catch (Throwable te) {
				log.error("serialize exception response fail. getId: {}", responseMessage.getId(), te);
				return;
			}
		}
		final int id = responseMessage.getId();
		final ResponseStatus status = responseMessage.getResponseStatus();
		connection.getChannel().writeAndFlush(responseMessage).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture channelFuture) throws Exception {
				if (channelFuture.isSuccess()) {
					if (log.isDebugEnabled()) {
						log.debug("write response success, getId={}, status={}", id, status);
					}
				}
				else {
					log.error("write response fail, getId={}, status={}", id, status, channelFuture.cause());
				}
			}
		});
	}

}
