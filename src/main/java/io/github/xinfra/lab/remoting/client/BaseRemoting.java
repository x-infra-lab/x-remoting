package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.netty.channel.ChannelFuture;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class BaseRemoting extends AbstractLifeCycle {
    private MessageFactory messageFactory;

    private Timer timer;

    public BaseRemoting(MessageFactory messageFactory) {
        this.messageFactory = messageFactory;

        // todo netty setup leakDetection
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
                            InvokeFuture future = connection.removeInvokeFuture(requestId);
                            if (future != null) {
                                future.complete(messageFactory.createSendFailResponseMessage(requestId,
                                        channelFuture.cause()));
                                log.error("Send message fail. id:{}", requestId, channelFuture.cause());
                            }
                        }
                    }
            );
        } catch (Throwable t) {
            InvokeFuture future = connection.removeInvokeFuture(requestId);
            if (future != null) {
                future.complete(messageFactory.createSendFailResponseMessage(requestId, t));
                log.error("Invoke sending message fail. id:{}", requestId, t);
            }
        }
        Message result;
        try {
            result = invokeFuture.get(timeoutMills, TimeUnit.MILLISECONDS);
        } catch (TimeoutException timeoutException) {
            connection.removeInvokeFuture(requestId);
            result = messageFactory.createTimeoutResponseMessage(requestId);
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
                Message result = messageFactory.createTimeoutResponseMessage(requestId);
                future.complete(result);
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
                                future.complete(messageFactory.createSendFailResponseMessage(requestId,
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
                future.complete(messageFactory.createSendFailResponseMessage(requestId, t));
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
                Message result = messageFactory.createTimeoutResponseMessage(requestId);
                future.complete(result);
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
                                future.complete(messageFactory.createSendFailResponseMessage(requestId,
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
                future.complete(messageFactory.createSendFailResponseMessage(requestId, t));
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

    @Override
    public void shutdown() {
        super.shutdown();
        Set<Timeout> timeouts = timer.stop();
        if (timeouts != null && !timeouts.isEmpty()) {
            log.warn("timer#stop with {} timeout unprocessed. timeouts:{}", timeouts.size(),
                    timeouts);
        }
    }
}
