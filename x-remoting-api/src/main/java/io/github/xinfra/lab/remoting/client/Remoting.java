package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.channel.ChannelFuture;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class Remoting {

	protected MessageFactory messageFactory;

	private final Timer timer;

	public Remoting(Protocol protocol) {
		this.messageFactory = protocol.messageFactory();
		this.timer = protocol.messageHandler().timer();
	}

	public ResponseMessage syncCall(RequestMessage requestMessage, Connection connection, int timeoutMills)
			throws InterruptedException {
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
						future.complete(messageFactory.createSendFailResponseMessage(requestId, channelFuture.cause(),
								connection.remoteAddress()));
					}
				}
			});
		}
		catch (Throwable t) {
			log.error("Invoke write requestMessage fail. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress(), t);
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.complete(messageFactory.createSendFailResponseMessage(requestId, t, connection.remoteAddress()));
			}
		}
		ResponseMessage responseMessage;
		try {
			responseMessage = invokeFuture.get(timeoutMills, TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException timeoutException) {
			log.warn("Wait responseMessage timeout. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress());
			connection.removeInvokeFuture(requestId);
			responseMessage = messageFactory.createTimeoutResponseMessage(requestId, connection.remoteAddress());
		}
		return responseMessage;
	}

	public InvokeFuture<? extends ResponseMessage> asyncCall(RequestMessage requestMessage, Connection connection,
			int timeoutMills) {
		int requestId = requestMessage.id();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestId);

		Timeout timeout = timer.newTimeout((t) -> {
			log.warn("Wait responseMessage timeout. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress());
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				ResponseMessage responseMessage = messageFactory.createTimeoutResponseMessage(requestId,
						connection.remoteAddress());
				future.complete(responseMessage);
			}
		}, timeoutMills, TimeUnit.MILLISECONDS);
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
						future.complete(messageFactory.createSendFailResponseMessage(requestId, channelFuture.cause(),
								connection.remoteAddress()));
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
				future.complete(messageFactory.createSendFailResponseMessage(requestId, t, connection.remoteAddress()));
			}
		}

		return invokeFuture;
	}

	public void asyncCall(RequestMessage requestMessage, Connection connection, int timeoutMills,
			InvokeCallBack invokeCallBack) {
		int requestId = requestMessage.id();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestId);

		Timeout timeout = timer.newTimeout((t) -> {
			log.warn("Wait responseMessage timeout. requestId:{} remoteAddress:{}", requestId,
					connection.remoteAddress());
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				ResponseMessage responseMessage = messageFactory.createTimeoutResponseMessage(requestId,
						connection.remoteAddress());
				future.complete(responseMessage);
				future.asyncExecuteCallBack(connection.getProtocol().messageHandler().executor(responseMessage));
			}
		}, timeoutMills, TimeUnit.MILLISECONDS);
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
						ResponseMessage responseMessage = messageFactory.createSendFailResponseMessage(requestId,
								channelFuture.cause(), connection.remoteAddress());
						future.complete(responseMessage);
						future
							.asyncExecuteCallBack(connection.getProtocol().messageHandler().executor(responseMessage));
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
				ResponseMessage responseMessage = messageFactory.createSendFailResponseMessage(requestId, t,
						connection.remoteAddress());
				future.complete(responseMessage);
				future.asyncExecuteCallBack(connection.getProtocol().messageHandler().executor(responseMessage));
			}
		}

	}

	public void oneway(RequestMessage requestMessage, Connection connection) {
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
