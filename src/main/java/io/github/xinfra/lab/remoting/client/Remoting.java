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

	public ResponseMessage syncCall(RequestMessage requestMessage, Connection connection, int timeoutMills) throws InterruptedException {
		int requestId = requestMessage.id();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestId, connection.getProtocol());
		try {
			connection.addInvokeFuture(invokeFuture);
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture channelFuture) -> {
				if (!channelFuture.isSuccess()) {
					InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
					if (future != null) {
						future.complete(messageFactory.createSendFailResponseMessage(requestId, channelFuture.cause(),
								connection.remoteAddress()));
						log.error("Send requestMessage fail. id:{} remoteAddress:{}", requestId, connection.remoteAddress(),
								channelFuture.cause());
					}
				}
			});
		}
		catch (Throwable t) {
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.complete(messageFactory.createSendFailResponseMessage(requestId, t, connection.remoteAddress()));
				log.error("Invoke sending requestMessage fail. id:{} remoteAddress:{}", requestId, connection.remoteAddress(),
						t);
			}
		}
		ResponseMessage responseMessage;
		try {
			responseMessage = invokeFuture.get(timeoutMills, TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException timeoutException) {
			connection.removeInvokeFuture(requestId);
			responseMessage = messageFactory.createTimeoutResponseMessage(requestId, connection.remoteAddress());
			log.warn("Wait result timeout. id:{} remoteAddress:{}", requestId, connection.remoteAddress());
		}
		return responseMessage;
	}

	public InvokeFuture<? extends ResponseMessage> asyncCall(RequestMessage requestMessage, Connection connection, int timeoutMills) {
		int requestId = requestMessage.id();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestId, connection.getProtocol());

		Timeout timeout = timer.newTimeout((t) -> {
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				ResponseMessage responseMessage = messageFactory.createTimeoutResponseMessage(requestId, connection.remoteAddress());
				future.complete(responseMessage);
			}
			log.warn("Wait result timeout. id:{} remoteAddress:{}", requestId, connection.remoteAddress());
		}, timeoutMills, TimeUnit.MILLISECONDS);
		invokeFuture.addTimeout(timeout);

		try {
			connection.addInvokeFuture(invokeFuture);
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture channelFuture) -> {
				if (!channelFuture.isSuccess()) {
					InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
					if (future != null) {
						future.cancelTimeout();
						future.complete(messageFactory.createSendFailResponseMessage(requestId, channelFuture.cause(),
								connection.remoteAddress()));
					}
					log.error("Send requestMessage fail. id:{} remoteAddress:{}", requestId, connection.remoteAddress(),
							channelFuture.cause());
				}
			});
		}
		catch (Throwable t) {
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.cancelTimeout();
				future.complete(messageFactory.createSendFailResponseMessage(requestId, t, connection.remoteAddress()));
			}
			log.error("Invoke sending requestMessage fail. id:{} remoteAddress:{}", requestId, connection.remoteAddress(), t);
		}

		return invokeFuture;
	}

	public void asyncCall(RequestMessage requestMessage, Connection connection, int timeoutMills, InvokeCallBack invokeCallBack) {
		int requestId = requestMessage.id();
		InvokeFuture<?> invokeFuture = new InvokeFuture<>(requestId, connection.getProtocol());

		Timeout timeout = timer.newTimeout((t) -> {
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				ResponseMessage responseMessage = messageFactory.createTimeoutResponseMessage(requestId, connection.remoteAddress());
				future.complete(responseMessage);
				future.asyncExecuteCallBack();
			}
			log.warn("Wait result timeout. id:{} remoteAddress:{}", requestId, connection.remoteAddress());
		}, timeoutMills, TimeUnit.MILLISECONDS);
		invokeFuture.addTimeout(timeout);
		invokeFuture.addCallBack(invokeCallBack);

		try {
			connection.addInvokeFuture(invokeFuture);
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture channelFuture) -> {
				if (!channelFuture.isSuccess()) {
					InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
					if (future != null) {
						future.cancelTimeout();
						future.complete(messageFactory.createSendFailResponseMessage(requestId, channelFuture.cause(),
								connection.remoteAddress()));
						future.asyncExecuteCallBack();
					}
					log.error("Send requestMessage fail. id:{} remoteAddress:{}", requestId, connection.remoteAddress(),
							channelFuture.cause());
				}
			});
		}
		catch (Throwable t) {
			InvokeFuture<?> future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.cancelTimeout();
				future.complete(messageFactory.createSendFailResponseMessage(requestId, t, connection.remoteAddress()));
				future.asyncExecuteCallBack();
			}
			log.error("Invoke sending requestMessage fail. id:{} remoteAddress:{}", requestId, connection.remoteAddress(), t);
		}

	}

	public void oneway(RequestMessage requestMessage, Connection connection) {
		int requestId = requestMessage.id();
		try {
			connection.getChannel().writeAndFlush(requestMessage).addListener((ChannelFuture future) -> {
				if (!future.isSuccess()) {
					log.error("Send requestMessage fail. id:{} remoteAddress:{}", requestId, connection.remoteAddress(),
							future.cause());
				} else {
					log.debug("Send requestMessage success. id:{} remoteAddress:{}", requestId, connection.remoteAddress());
				}
			});
		}
		catch (Throwable t) {
			log.error("Invoke sending requestMessage fail. id:{} remoteAddress:{}", requestId, connection.remoteAddress(), t);
		}
	}

}
