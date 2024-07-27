package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.common.Until;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class BaseRemotingTest {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    private ProtocolType test = new ProtocolType("BaseRemotingTest", new byte[]{0xb});

    @Test
    public void testSyncCall() throws InterruptedException, TimeoutException {
        int requestId = IDGenerator.nextRequestId();
        MessageFactory messageFactory = mock(MessageFactory.class);
        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);

        Message message = mock(Message.class);
        doReturn(requestId).when(message).id();
        Endpoint endpoint = mock(Endpoint.class);
        Channel channel = new EmbeddedChannel();
        doReturn(test).when(endpoint).getProtocolType();
        Connection connection = new Connection(endpoint, channel);

        Message result = mock(Message.class);
        // complete invokeFuture
        executor.submit(() -> {
            try {
                Until.untilIsTrue(
                        () -> {
                            InvokeFuture invokeFuture = connection.removeInvokeFuture(requestId);
                            if (invokeFuture != null) {
                                invokeFuture.complete(result);
                                return true;
                            }
                            return false;
                        }, 30, 100
                );
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        });

        Message msg = baseRemoting.syncCall(message, connection, 1000);

        Assertions.assertTrue(msg == result);
    }


    @Test
    public void testSyncCallSendFailed1() throws InterruptedException, TimeoutException {
        int requestId = IDGenerator.nextRequestId();
        MessageFactory messageFactory = mock(MessageFactory.class);

        Message sendFailedMessage = mock(Message.class);
        doReturn(sendFailedMessage).when(messageFactory).createSendFailResponseMessage(anyInt(), any(), any());

        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        Message message = mock(Message.class);
        doReturn(requestId).when(message).id();
        Endpoint endpoint = mock(Endpoint.class);
        Channel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(test).when(endpoint).getProtocolType();
        Connection connection = new Connection(endpoint, channel);

        doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

        Message msg = baseRemoting.syncCall(message, connection, 1000);
        Assertions.assertTrue(msg == sendFailedMessage);
    }

    @Test
    public void testSyncCallSendFailed2() throws InterruptedException, TimeoutException {
        int requestId = IDGenerator.nextRequestId();
        MessageFactory messageFactory = mock(MessageFactory.class);

        Message sendFailedMessage = mock(Message.class);
        doReturn(sendFailedMessage).when(messageFactory).createSendFailResponseMessage(anyInt(), any(), any());

        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        Message message = mock(Message.class);
        doReturn(requestId).when(message).id();
        Endpoint endpoint = mock(Endpoint.class);
        Channel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(test).when(endpoint).getProtocolType();
        Connection connection = new Connection(endpoint, channel);

        doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

        Message msg = baseRemoting.syncCall(message, connection, 1000);
        Assertions.assertTrue(msg == sendFailedMessage);
    }

    @Test
    public void testSyncCallTimeout() throws InterruptedException, TimeoutException {
        int requestId = IDGenerator.nextRequestId();
        MessageFactory messageFactory = mock(MessageFactory.class);

        Message timeoutMessage = mock(Message.class);
        doReturn(timeoutMessage).when(messageFactory).createTimeoutResponseMessage(anyInt(), any());

        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        Message message = mock(Message.class);
        doReturn(requestId).when(message).id();
        Endpoint endpoint = mock(Endpoint.class);
        Channel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(test).when(endpoint).getProtocolType();
        Connection connection = new Connection(endpoint, channel);
        Message msg = baseRemoting.syncCall(message, connection, 1000);
        Assertions.assertTrue(msg == timeoutMessage);
    }

    @Test
    public void testAsyncCall() throws InterruptedException {
        int requestId = IDGenerator.nextRequestId();
        MessageFactory messageFactory = mock(MessageFactory.class);

        Message result = mock(Message.class);

        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        Message message = mock(Message.class);
        doReturn(requestId).when(message).id();
        Endpoint endpoint = mock(Endpoint.class);
        Channel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(test).when(endpoint).getProtocolType();
        Connection connection = new Connection(endpoint, channel);
        InvokeFuture invokeFuture = baseRemoting.asyncCall(message, connection, 1000);

        // complete invokeFuture
        executor.submit(() -> {
            try {
                Until.untilIsTrue(
                        () -> {
                            InvokeFuture future = connection.removeInvokeFuture(requestId);
                            if (future != null) {
                                future.complete(result);
                                return true;
                            }
                            return false;
                        }, 30, 100
                );
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (TimeoutException e) {
                throw new RuntimeException(e);
            }
        });

        Assertions.assertTrue(invokeFuture.get() == result);

    }

    @Test
    public void testAsyncCallSendFailed1() throws InterruptedException, TimeoutException {
        int requestId = IDGenerator.nextRequestId();
        MessageFactory messageFactory = mock(MessageFactory.class);

        Message sendFailedMessage = mock(Message.class);
        doReturn(sendFailedMessage).when(messageFactory).createSendFailResponseMessage(anyInt(), any(), any());

        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        Message message = mock(Message.class);
        doReturn(requestId).when(message).id();
        Endpoint endpoint = mock(Endpoint.class);
        Channel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(test).when(endpoint).getProtocolType();
        Connection connection = new Connection(endpoint, channel);

        doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

        InvokeFuture invokeFuture = baseRemoting.asyncCall(message, connection, 1000);
        Assertions.assertTrue(invokeFuture.get() == sendFailedMessage);
    }

    @Test
    public void testAsyncCallSendFailed2() throws InterruptedException, TimeoutException {
        int requestId = IDGenerator.nextRequestId();
        MessageFactory messageFactory = mock(MessageFactory.class);

        Message sendFailedMessage = mock(Message.class);
        doReturn(sendFailedMessage).when(messageFactory).createSendFailResponseMessage(anyInt(), any(), any());

        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        Message message = mock(Message.class);
        doReturn(requestId).when(message).id();
        Endpoint endpoint = mock(Endpoint.class);
        Channel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(test).when(endpoint).getProtocolType();
        Connection connection = new Connection(endpoint, channel);

        doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

        InvokeFuture invokeFuture = baseRemoting.asyncCall(message, connection, 1000);
        Assertions.assertTrue(invokeFuture.get() == sendFailedMessage);
    }

    @Test
    public void testAsyncCallTimeout() throws InterruptedException, TimeoutException {
        int requestId = IDGenerator.nextRequestId();
        MessageFactory messageFactory = mock(MessageFactory.class);

        Message timeoutMessage = mock(Message.class);
        doReturn(timeoutMessage).when(messageFactory).createTimeoutResponseMessage(anyInt(), any());

        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        Message message = mock(Message.class);
        doReturn(requestId).when(message).id();
        Endpoint endpoint = mock(Endpoint.class);
        Channel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(test).when(endpoint).getProtocolType();
        Connection connection = new Connection(endpoint, channel);
        InvokeFuture invokeFuture = baseRemoting.asyncCall(message, connection, 1000);
        Assertions.assertTrue(invokeFuture.get() == timeoutMessage);
    }

    @Test
    public void testAsyncCallWithCallBack() throws InterruptedException {
        int requestId = IDGenerator.nextRequestId();
        MessageFactory messageFactory = mock(MessageFactory.class);

        Message result = mock(Message.class);

        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        Message message = mock(Message.class);
        doReturn(requestId).when(message).id();
        Endpoint endpoint = mock(Endpoint.class);
        Channel channel = new EmbeddedChannel();
        channel = spy(channel);
        doReturn(test).when(endpoint).getProtocolType();
        Connection connection = new Connection(endpoint, channel);

        AtomicReference<Message> callbackMessage = new AtomicReference<>();
        baseRemoting.asyncCall(message, connection, 1000,
                (msg) -> {
                    callbackMessage.set(msg);
                });

        // complete invokeFuture
        InvokeFuture future = connection.removeInvokeFuture(requestId);
        future.cancelTimeout();
        future.complete(result);
        future.executeCallBack();

        Assertions.assertTrue(callbackMessage.get() == result);

    }
}
