package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.rpc.message.MessageFactory;
import io.github.xinfra.lab.remoting.rpc.message.RequestMessage;
import io.github.xinfra.lab.remoting.rpc.message.ResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.rpc.protocol.RpcProtocol;
import io.netty.channel.ChannelFuture;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Call {

	Logger log = LoggerFactory.getLogger(Call.class);

	default ResponseMessage blockingCall(RequestMessage requestMessage, Connection connection, CallOptions callOptions)
			throws InterruptedException {
		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		MessageFactory messageFactory = protocol.getMessageFactory();
		InFlightRequests inFlight = InFlightRequests.getOrCreate(connection);

		int requestId = requestMessage.getId();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestMessage);
		try {
			inFlight.add(invokeFuture);
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture channelFuture) -> {
				if (!channelFuture.isSuccess()) {
					log.error("Write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
							connection.remoteAddress(), channelFuture.cause());
					InvokeFuture<?> future = inFlight.remove(requestId);
					if (future != null) {
						future.complete(messageFactory.createResponse(requestId, requestMessage.getSerializationType(),
								ResponseStatus.SendFailed, channelFuture.cause()));
					}
				}
			});
		}
		catch (Throwable t) {
			log.error("Invoke write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress(), t);
			InvokeFuture<?> future = inFlight.remove(requestId);
			if (future != null) {
				future.complete(messageFactory.createResponse(requestId, requestMessage.getSerializationType(),
						ResponseStatus.SendFailed, t));
			}
		}
		ResponseMessage responseMessage;
		try {
			responseMessage = invokeFuture.get(callOptions.getTimeoutMills(), TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException timeoutException) {
			log.warn("Wait responseMessage timeout. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress());
			inFlight.remove(requestId);
			responseMessage = messageFactory.createResponse(requestId, requestMessage.getSerializationType(),
					ResponseStatus.Timeout);
		}
		return responseMessage;
	}

	default InvokeFuture<? extends ResponseMessage> futureCall(RequestMessage requestMessage, Connection connection,
			CallOptions callOptions) {
		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		Timer timer = connection.getTimer();
		MessageFactory messageFactory = protocol.getMessageFactory();
		InFlightRequests inFlight = InFlightRequests.getOrCreate(connection);

		int requestId = requestMessage.getId();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestMessage);
		Timeout timeout = timer.newTimeout((t) -> {
			log.warn("Wait responseMessage timeout. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress());
			InvokeFuture<?> future = inFlight.remove(requestId);
			if (future != null) {
				ResponseMessage responseMessage = messageFactory.createResponse(requestId,
						requestMessage.getSerializationType(), ResponseStatus.Timeout);
				future.complete(responseMessage);
			}
		}, callOptions.getTimeoutMills(), TimeUnit.MILLISECONDS);
		invokeFuture.addTimeout(timeout);

		try {
			inFlight.add(invokeFuture);
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture channelFuture) -> {
				if (!channelFuture.isSuccess()) {
					log.error("Write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
							connection.remoteAddress(), channelFuture.cause());
					InvokeFuture<?> future = inFlight.remove(requestId);
					if (future != null) {
						future.cancelTimeout();
						future.complete(messageFactory.createResponse(requestId, requestMessage.getSerializationType(),
								ResponseStatus.SendFailed, channelFuture.cause()));
					}
				}
			});
		}
		catch (Throwable t) {
			log.error("Invoke write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress(), t);
			InvokeFuture<?> future = inFlight.remove(requestId);
			if (future != null) {
				future.cancelTimeout();
				future.complete(messageFactory.createResponse(requestId, requestMessage.getSerializationType(),
						ResponseStatus.SendFailed, t));
			}
		}

		return invokeFuture;
	}

	default void asyncCall(RequestMessage requestMessage, Connection connection, CallOptions callOptions,
			InvokeCallBack invokeCallBack) {
		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		MessageFactory messageFactory = protocol.getMessageFactory();
		Timer timer = connection.getTimer();
		InFlightRequests inFlight = InFlightRequests.getOrCreate(connection);

		int requestId = requestMessage.getId();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestMessage);
		Timeout timeout = timer.newTimeout((t) -> {
			log.warn("Wait responseMessage timeout. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress());
			InvokeFuture<?> future = inFlight.remove(requestId);
			if (future != null) {
				ResponseMessage responseMessage = messageFactory.createResponse(requestId,
						requestMessage.getSerializationType(), ResponseStatus.Timeout);
				future.complete(responseMessage);
				future.executeCallBack(connection.getExecutor());
			}
		}, callOptions.getTimeoutMills(), TimeUnit.MILLISECONDS);
		invokeFuture.addTimeout(timeout);
		invokeFuture.addCallBack(invokeCallBack);

		try {
			inFlight.add(invokeFuture);
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture channelFuture) -> {
				if (!channelFuture.isSuccess()) {
					log.error("Write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
							connection.remoteAddress(), channelFuture.cause());
					InvokeFuture<?> future = inFlight.remove(requestId);
					if (future != null) {
						future.cancelTimeout();
						ResponseMessage responseMessage = messageFactory.createResponse(requestId,
								requestMessage.getSerializationType(), ResponseStatus.SendFailed,
								channelFuture.cause());
						future.complete(responseMessage);
						future.executeCallBack(connection.getExecutor());
					}

				}
			});
		}
		catch (Throwable t) {
			log.error("Invoke write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress(), t);
			InvokeFuture<?> future = inFlight.remove(requestId);
			if (future != null) {
				future.cancelTimeout();
				ResponseMessage responseMessage = messageFactory.createResponse(requestId,
						requestMessage.getSerializationType(), ResponseStatus.SendFailed, t);
				future.complete(responseMessage);
				future.executeCallBack(connection.getExecutor());
			}
		}

	}

	default void oneway(RequestMessage requestMessage, Connection connection, CallOptions callOptions) {
		int requestId = requestMessage.getId();
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
