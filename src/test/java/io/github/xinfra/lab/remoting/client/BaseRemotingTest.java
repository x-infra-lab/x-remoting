package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class BaseRemotingTest {

	ExecutorService executor;

	Timer timer;

	TestProtocol testProtocol;

	BaseRemoting baseRemoting;

	int requestId;

	MessageFactory messageFactory;

	@BeforeEach
	public void beforeEach() {
		testProtocol = new TestProtocol();
		executor = Executors.newCachedThreadPool();
		// set message handler

		MessageHandler messageHandler = mock(MessageHandler.class);
		doReturn(executor).when(messageHandler).executor();

		timer = new HashedWheelTimer();
		doReturn(timer).when(messageHandler).timer();
		testProtocol.setTestMessageHandler(messageHandler);

		messageFactory = mock(MessageFactory.class);
		testProtocol.setTestMessageFactory(messageFactory);

		requestId = IDGenerator.nextRequestId();
		baseRemoting = new BaseRemoting(testProtocol);
	}

	@AfterEach
	public void afterEach() {
		executor.shutdownNow();
		timer.stop();
	}

	@Test
	public void testSyncCall() throws InterruptedException {
		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();

		Channel channel = new EmbeddedChannel();
		Connection connection = new Connection(testProtocol, channel);

		Message result = mock(Message.class);
		// complete invokeFuture
		executor.submit(() -> {
			try {
				Wait.untilIsTrue(() -> {
					InvokeFuture invokeFuture = connection.removeInvokeFuture(requestId);
					if (invokeFuture != null) {
						invokeFuture.complete(result);
						return true;
					}
					return false;
				}, 30, 100);
			}
			catch (InterruptedException | TimeoutException e) {
				throw new RuntimeException(e);
			}
		});

		Message msg = baseRemoting.syncCall(message, connection, 1000);

		Assertions.assertTrue(msg == result);
	}

	@Test
	public void testSyncCallSendFailed1() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailResponseMessage(anyInt(), any(), any());

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		Message msg = baseRemoting.syncCall(message, connection, 1000);
		Assertions.assertTrue(msg == sendFailedMessage);
	}

	@Test
	public void testSyncCallSendFailed2() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailResponseMessage(anyInt(), any(), any());

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		Message msg = baseRemoting.syncCall(message, connection, 1000);
		Assertions.assertTrue(msg == sendFailedMessage);
	}

	@Test
	public void testSyncCallTimeout() throws InterruptedException, TimeoutException {

		Message timeoutMessage = mock(Message.class);
		doReturn(timeoutMessage).when(messageFactory).createTimeoutResponseMessage(anyInt(), any());

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);
		Message msg = baseRemoting.syncCall(message, connection, 1000);
		Assertions.assertTrue(msg == timeoutMessage);
	}

	@Test
	public void testAsyncCall() throws InterruptedException, TimeoutException {

		Message result = mock(Message.class);

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);
		InvokeFuture invokeFuture = baseRemoting.asyncCall(message, connection, 1000);

		// complete invokeFuture
		Wait.untilIsTrue(() -> {
			InvokeFuture future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.complete(result);
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(invokeFuture.get() == result);

	}

	@Test
	public void testAsyncCallSendFailed1() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailResponseMessage(anyInt(), any(), any());

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		InvokeFuture invokeFuture = baseRemoting.asyncCall(message, connection, 1000);
		Assertions.assertTrue(invokeFuture.get() == sendFailedMessage);
	}

	@Test
	public void testAsyncCallSendFailed2() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailResponseMessage(anyInt(), any(), any());

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		InvokeFuture invokeFuture = baseRemoting.asyncCall(message, connection, 1000);
		Assertions.assertTrue(invokeFuture.get() == sendFailedMessage);
	}

	@Test
	public void testAsyncCallTimeout() throws InterruptedException, TimeoutException {

		Message timeoutMessage = mock(Message.class);
		doReturn(timeoutMessage).when(messageFactory).createTimeoutResponseMessage(anyInt(), any());

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);
		InvokeFuture invokeFuture = baseRemoting.asyncCall(message, connection, 1000);
		Assertions.assertTrue(invokeFuture.get() == timeoutMessage);
	}

	@Test
	public void testAsyncCallWithCallback() throws InterruptedException {

		Message result = mock(Message.class);

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		AtomicReference<Message> callbackMessage = new AtomicReference<>();
		baseRemoting.asyncCall(message, connection, 1000, (msg) -> {
			callbackMessage.set(msg);
		});

		// complete invokeFuture
		InvokeFuture future = connection.removeInvokeFuture(requestId);
		future.cancelTimeout();
		future.complete(result);
		future.executeCallBack();

		Assertions.assertTrue(callbackMessage.get() == result);
	}

	@Test
	public void testAsyncCallWithCallbackSendFailed1() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailResponseMessage(anyInt(), any(), any());

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		AtomicReference<Message> callbackMessage = new AtomicReference<>();
		baseRemoting.asyncCall(message, connection, 1000, (msg) -> {
			callbackMessage.set(msg);
		});

		Wait.untilIsTrue(() -> {
			if (callbackMessage.get() != null) {
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(callbackMessage.get() == sendFailedMessage);
	}

	@Test
	public void testAsyncCallWithCallbackSendFailed2() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailResponseMessage(anyInt(), any(), any());

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		AtomicReference<Message> callbackMessage = new AtomicReference<>();
		baseRemoting.asyncCall(message, connection, 1000, (msg) -> {
			callbackMessage.set(msg);
		});

		Wait.untilIsTrue(() -> {
			if (callbackMessage.get() != null) {
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(callbackMessage.get() == sendFailedMessage);
	}

	@Test
	public void testAsyncCallWithCallbackTimeout() throws InterruptedException, TimeoutException {

		Message timeoutMessage = mock(Message.class);
		doReturn(timeoutMessage).when(messageFactory).createTimeoutResponseMessage(anyInt(), any());

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		AtomicReference<Message> callbackMessage = new AtomicReference<>();
		baseRemoting.asyncCall(message, connection, 1000, (msg) -> {
			callbackMessage.set(msg);
		});

		Wait.untilIsTrue(() -> {
			if (callbackMessage.get() != null) {
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(callbackMessage.get() == timeoutMessage);
	}

	@Test
	public void testOneway() throws InterruptedException, TimeoutException {

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);
		baseRemoting.oneway(message, connection);

		// complete invokeFuture
		Channel finalChannel = channel;
		Wait.untilIsTrue(() -> {
			try {
				verify(finalChannel, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable e) {
				return false;
			}
		}, 30, 100);

		verify(channel, times(1)).writeAndFlush(eq(message));
	}

	@Test
	public void testOnewaySendFailed1() throws InterruptedException, TimeoutException {

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		baseRemoting.oneway(message, connection);

		// complete invokeFuture
		Channel finalChannel = channel;
		Wait.untilIsTrue(() -> {
			try {
				verify(finalChannel, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable e) {
				return false;
			}
		}, 30, 100);

		verify(channel, times(1)).writeAndFlush(eq(message));
	}

	@Test
	public void testOnewaySendFailed2() throws InterruptedException, TimeoutException {

		Message message = mock(Message.class);
		doReturn(requestId).when(message).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		baseRemoting.oneway(message, connection);

		// complete invokeFuture
		Channel finalChannel = channel;
		Wait.untilIsTrue(() -> {
			try {
				verify(finalChannel, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable e) {
				return false;
			}
		}, 30, 100);

		verify(channel, times(1)).writeAndFlush(eq(message));
	}

}
