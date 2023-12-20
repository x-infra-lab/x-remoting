package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.netty.channel.ChannelFuture;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseRemoting {
    private MessageFactory messageFactory;

    private Timer timer;

    public BaseRemoting(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;
        this.timer = new HashedWheelTimer();
    }

    public Message syncCall(Message message, Connection connection, int timeoutMills) throws InterruptedException {
        int requestId = message.id();
        InvokeFuture invokeFuture = new InvokeFuture(requestId);
        try {
            connection.addInvokeFuture(invokeFuture);
            connection.getChannel().writeAndFlush(message).addListener(
                    (ChannelFuture channelFuture) -> {
                        if (!channelFuture.isSuccess()) {
                            connection.removeInvokeFuture(requestId);
                            invokeFuture.finish(messageFactory.createSendFailResponseMessage(connection.remoteAddress(),
                                    channelFuture.cause()));
                            log.error("Send message fail. id:{}", requestId, channelFuture.cause());
                        }
                    }
            );
        } catch (Throwable t) {
            connection.removeInvokeFuture(requestId);
            invokeFuture.finish(messageFactory.createSendFailResponseMessage(connection.remoteAddress(), t));
            log.error("Invoke sending message fail. id:{}", requestId, t);
        }

        Message result = invokeFuture.await(timeoutMills, TimeUnit.MILLISECONDS);

        if (result == null) {
            connection.removeInvokeFuture(requestId);
            result = messageFactory.createTimeoutResponseMessage(connection.remoteAddress());
            log.warn("Wait result timeout. id:{}", requestId);
        }
        return result;
    }

    public InvokeFuture asyncCall(Message message, Connection connection, int timeoutMills) {
        int requestId = message.id();
        InvokeFuture invokeFuture = new InvokeFuture(requestId);

        Timeout timeout = timer.newTimeout((t) -> {
            InvokeFuture future = connection.removeInvokeFuture(requestId);
            if (future != null) {
                Message result = messageFactory.createTimeoutResponseMessage(connection.remoteAddress());
                future.finish(result);
            }
            log.warn("Wait result timeout. id:{}", requestId);
        }, timeoutMills, TimeUnit.MILLISECONDS);
        invokeFuture.addTimeout(timeout);

        try {
            connection.addInvokeFuture(invokeFuture);
            connection.getChannel().writeAndFlush(message).addListener(
                    (ChannelFuture channelFuture) -> {
                        if (!channelFuture.isSuccess()) {
                            InvokeFuture future = connection.removeInvokeFuture(requestId);
                            if (future != null) {
                                future.cancelTimeout();
                                future.finish(messageFactory.createSendFailResponseMessage(connection.remoteAddress(),
                                        channelFuture.cause()));
                            }
                            log.error("Send message fail. id:{}", requestId, channelFuture.cause());
                        }
                    }
            );
        } catch (Throwable t) {
            InvokeFuture future = connection.removeInvokeFuture(requestId);
            if (future != null) {
                future.cancelTimeout();
                future.finish(messageFactory.createSendFailResponseMessage(connection.remoteAddress(), t));
            }
            log.error("Invoke sending message fail. id:{}", requestId, t);
        }

        return invokeFuture;
    }

    public void asyncCall(Message message, Connection connection,
                          int timeoutMills,
                          InvokeCallBack invokeCallBack) {
        int requestId = message.id();
        InvokeFuture invokeFuture = new InvokeFuture(requestId);

        Timeout timeout = timer.newTimeout((t) -> {
            InvokeFuture future = connection.removeInvokeFuture(requestId);
            if (future != null) {
                Message result = messageFactory.createTimeoutResponseMessage(connection.remoteAddress());
                future.finish(result);
                future.asyncExecuteCallBack();
            }
            log.warn("Wait result timeout. id:{}", requestId);
        }, timeoutMills, TimeUnit.MILLISECONDS);
        invokeFuture.addTimeout(timeout);
        invokeFuture.addCallBack(invokeCallBack);

        try {
            connection.addInvokeFuture(invokeFuture);
            connection.getChannel().writeAndFlush(message).addListener(
                    (ChannelFuture channelFuture) -> {
                        if (!channelFuture.isSuccess()) {
                            InvokeFuture future = connection.removeInvokeFuture(requestId);
                            if (future != null) {
                                future.cancelTimeout();
                                future.finish(messageFactory.createSendFailResponseMessage(connection.remoteAddress(),
                                        channelFuture.cause()));
                                future.asyncExecuteCallBack();
                            }
                            log.error("Send message fail. id:{}", requestId, channelFuture.cause());
                        }
                    }
            );
        } catch (Throwable t) {
            InvokeFuture future = connection.removeInvokeFuture(requestId);
            if (future != null) {
                future.cancelTimeout();
                future.finish(messageFactory.createSendFailResponseMessage(connection.remoteAddress(), t));
                future.asyncExecuteCallBack();
            }
            log.error("Invoke sending message fail. id:{}", requestId, t);
        }

    }

    public void oneway(Message message, Connection connection) {
        int requestId = message.id();
        try {
            connection.getChannel().writeAndFlush(message).addListener(
                    (ChannelFuture future) -> {
                        if (!future.isSuccess()) {
                            log.error("Send message fail. id:{}", requestId, future.cause());
                        }
                    }
            );
        } catch (Throwable t) {
            log.error("Invoke sending message fail. id:{}", requestId, t);
        }
    }
}
