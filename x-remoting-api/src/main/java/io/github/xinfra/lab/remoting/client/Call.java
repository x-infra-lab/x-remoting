package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.channel.ChannelFuture;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Call {

	Logger log = LoggerFactory.getLogger(Call.class);

	default ResponseMessage syncCall(RequestMessage requestMessage, Connection connection, CallOptions callOptions)
			throws InterruptedException {
		Protocol protocol = connection.getProtocol();
		MessageFactory messageFactory = protocol.messageFactory();

		int requestId = requestMessage.id();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestId);
		try {
			connection.addInvokeFuture(invokeFuture);
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture channelFuture) -> {
				if (!channelFuture.isSuccess()) {
					log.error("Write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
							connection.remoteAddress(), channelFuture.cause());
					InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
					if (future != null) {
						future.complete(messageFactory.createResponse(requestId, ResponseStatus.SendFailed,
								channelFuture.cause()));
					}
				}
			});
		}
		catch (Throwable t) {
			log.error("Invoke write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress(), t);
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.complete(messageFactory.createResponse(requestId, ResponseStatus.SendFailed, t));
			}
		}
		ResponseMessage responseMessage;
		try {
			responseMessage = invokeFuture.get(callOptions.getTimeoutMills(), TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException timeoutException) {
			log.warn("Wait responseMessage timeout. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress());
			connection.removeInvokeFuture(requestId);
			responseMessage = messageFactory.createResponse(requestId, ResponseStatus.Timeout);
		}
		return responseMessage;
	}

	default InvokeFuture<? extends ResponseMessage> asyncCall(RequestMessage requestMessage, Connection connection,
			CallOptions callOptions) {
		Protocol protocol = connection.getProtocol();
		Timer timer = connection.getTimer();
		MessageFactory messageFactory = protocol.messageFactory();

		int requestId = requestMessage.id();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestId);
		Timeout timeout = timer.newTimeout((t) -> {
			log.warn("Wait responseMessage timeout. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress());
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				ResponseMessage responseMessage = messageFactory.createResponse(requestId, ResponseStatus.Timeout);
				future.complete(responseMessage);
			}
		}, callOptions.getTimeoutMills(), TimeUnit.MILLISECONDS);
		invokeFuture.addTimeout(timeout);

		try {
			connection.addInvokeFuture(invokeFuture);
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture channelFuture) -> {
				if (!channelFuture.isSuccess()) {
					log.error("Write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
							connection.remoteAddress(), channelFuture.cause());
					InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
					if (future != null) {
						future.cancelTimeout();
						future.complete(messageFactory.createResponse(requestId, ResponseStatus.SendFailed,
								channelFuture.cause()));
					}
				}
			});
		}
		catch (Throwable t) {
			log.error("Invoke write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress(), t);
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.cancelTimeout();
				future.complete(messageFactory.createResponse(requestId, ResponseStatus.SendFailed, t));
			}
		}

		return invokeFuture;
	}

	default void asyncCall(RequestMessage requestMessage, Connection connection, CallOptions callOptions,
			InvokeCallBack invokeCallBack) {
		Protocol protocol = connection.getProtocol();
		MessageFactory messageFactory = protocol.messageFactory();
		Timer timer = connection.getTimer();

		int requestId = requestMessage.id();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestId);
		Timeout timeout = timer.newTimeout((t) -> {
			log.warn("Wait responseMessage timeout. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress());
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				ResponseMessage responseMessage = messageFactory.createResponse(requestId, ResponseStatus.Timeout);
				future.complete(responseMessage);
				future.asyncExecuteCallBack(connection.getExecutor());
			}
		}, callOptions.getTimeoutMills(), TimeUnit.MILLISECONDS);
		invokeFuture.addTimeout(timeout);
		invokeFuture.addCallBack(invokeCallBack);

		try {
			connection.addInvokeFuture(invokeFuture);
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture channelFuture) -> {
				if (!channelFuture.isSuccess()) {
					log.error("Write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
							connection.remoteAddress(), channelFuture.cause());
					InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
					if (future != null) {
						future.cancelTimeout();
						ResponseMessage responseMessage = messageFactory.createResponse(requestId,
								ResponseStatus.SendFailed, channelFuture.cause());
						future.complete(responseMessage);
						future.asyncExecuteCallBack(connection.getExecutor());
					}

				}
			});
		}
		catch (Throwable t) {
			log.error("Invoke write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress(), t);
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.cancelTimeout();
				ResponseMessage responseMessage = messageFactory.createResponse(requestId, ResponseStatus.SendFailed,
						t);
				future.complete(responseMessage);
				future.asyncExecuteCallBack(connection.getExecutor());
			}
		}

	}

	default void oneway(RequestMessage requestMessage, Connection connection, CallOptions callOptions) {
		int requestId = requestMessage.id();
		try {
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture future) -> {
				if (!future.isSuccess()) {
					log.error("Write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
							connection.remoteAddress(), future.cause());
				}
				else {
					log.debug("Write requestMessage success. requestId:{} remoteAddress:{}", requestId,
							connection.remoteAddress());
				}
			});
		}
		catch (Throwable t) {
			log.error("Invoke write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress(), t);
		}
	}

}
