package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Responses {

	public static void sendResponse(Connection connection, ResponseMessage responseMessage) {
		try {
			responseMessage.serialize();
		}
		catch (Throwable t) {
			log.error("responseMessage serialize fail.", t);
			responseMessage = connection.getProtocol()
				.messageFactory()
				.createResponse(responseMessage.id(), responseMessage.serializationType(), ResponseStatus.InternalError,
						t);
			try {
				responseMessage.serialize();
			}
			catch (Throwable te) {
				log.error("serialize exception response fail. id: {}", responseMessage.id(), te);
				return;
			}
		}
		final int id = responseMessage.id();
		final ResponseStatus status = responseMessage.responseStatus();
		connection.getChannel().writeAndFlush(responseMessage).addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture channelFuture) throws Exception {
				if (channelFuture.isSuccess()) {
					if (log.isDebugEnabled()) {
						log.info("write response success, id={}, status={}", id, status);
					}
				}
				else {
					log.error("write response fail, id={}, status={}", id, status, channelFuture.cause());
				}
			}
		});
	}

}
