package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
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

public class RemotingTest {

	ExecutorService executor;

	Timer timer;

	TestProtocol testProtocol;

	Remoting remoting;

	int requestId;

	MessageFactory messageFactory;

	@BeforeEach
	public void beforeEach() {
		testProtocol = new TestProtocol();
		executor = Executors.newCachedThreadPool();
		// set message handler

		MessageHandler messageHandler = mock(MessageHandler.class);

		timer = new HashedWheelTimer();
		doReturn(timer).when(messageHandler).timer();
		testProtocol.setTestMessageHandler(messageHandler);

		messageFactory = mock(MessageFactory.class);
		testProtocol.setTestMessageFactory(messageFactory);

		requestId = IDGenerator.nextRequestId();
		remoting = new Remoting(testProtocol);
	}

	@AfterEach
	public void afterEach() {
		executor.shutdownNow();
		timer.stop();
	}

	@Test
	public void testSyncCall() throws InterruptedException {
		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();

		Channel channel = new EmbeddedChannel();
		Connection connection = new Connection(testProtocol, channel);

		ResponseMessage responseMessage = mock(ResponseMessage.class);
		// complete invokeFuture
		executor.submit(() -> {
			try {
				Wait.untilIsTrue(() -> {
					InvokeFuture invokeFuture = connection.removeInvokeFuture(requestId);
					if (invokeFuture != null) {
						invokeFuture.complete(responseMessage);
						return true;
					}
					return false;
				}, 30, 100);
			}
			catch (InterruptedException | TimeoutException e) {
				throw new RuntimeException(e);
			}
		});

		Message msg = remoting.syncCall(requestMessage, connection, 1000);

		Assertions.assertTrue(msg == responseMessage);
	}

	@Test
	public void testSyncCallSendFailed1() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailedResponseMessage(anyInt(), any(), any());

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		Message msg = remoting.syncCall(requestMessage, connection, 1000);
		Assertions.assertTrue(msg == sendFailedMessage);
	}

	@Test
	public void testSyncCallSendFailed2() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailedResponseMessage(anyInt(), any(), any());

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		Message msg = remoting.syncCall(requestMessage, connection, 1000);
		Assertions.assertTrue(msg == sendFailedMessage);
	}

	@Test
	public void testSyncCallTimeout() throws InterruptedException, TimeoutException {

		Message timeoutMessage = mock(Message.class);
		doReturn(timeoutMessage).when(messageFactory).createTimeoutResponseMessage(anyInt(), any());

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);
		Message msg = remoting.syncCall(requestMessage, connection, 1000);
		Assertions.assertTrue(msg == timeoutMessage);
	}

	@Test
	public void testAsyncCall() throws InterruptedException, TimeoutException {

		ResponseMessage responseMessage = mock(ResponseMessage.class);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);
		InvokeFuture invokeFuture = remoting.asyncCall(requestMessage, connection, 1000);

		// complete invokeFuture
		Wait.untilIsTrue(() -> {
			InvokeFuture future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.complete(responseMessage);
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(invokeFuture.get() == responseMessage);

	}

	@Test
	public void testAsyncCallSendFailed1() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailedResponseMessage(anyInt(), any(), any());

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		InvokeFuture invokeFuture = remoting.asyncCall(requestMessage, connection, 1000);
		Assertions.assertTrue(invokeFuture.get() == sendFailedMessage);
	}

	@Test
	public void testAsyncCallSendFailed2() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailedResponseMessage(anyInt(), any(), any());

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		InvokeFuture invokeFuture = remoting.asyncCall(requestMessage, connection, 1000);
		Assertions.assertTrue(invokeFuture.get() == sendFailedMessage);
	}

	@Test
	public void testAsyncCallTimeout() throws InterruptedException, TimeoutException {

		Message timeoutMessage = mock(Message.class);
		doReturn(timeoutMessage).when(messageFactory).createTimeoutResponseMessage(anyInt(), any());

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);
		InvokeFuture invokeFuture = remoting.asyncCall(requestMessage, connection, 1000);
		Assertions.assertTrue(invokeFuture.get() == timeoutMessage);
	}

	@Test
	public void testAsyncCallWithCallback() throws InterruptedException {

		ResponseMessage responseMessage = mock(ResponseMessage.class);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		AtomicReference<Message> callbackMessage = new AtomicReference<>();
		remoting.asyncCall(requestMessage, connection, 1000, (msg) -> {
			callbackMessage.set(msg);
		});

		// complete invokeFuture
		InvokeFuture future = connection.removeInvokeFuture(requestId);
		future.cancelTimeout();
		future.complete(responseMessage);
		future.executeCallBack();

		Assertions.assertTrue(callbackMessage.get() == responseMessage);
	}

	@Test
	public void testAsyncCallWithCallbackSendFailed1() throws InterruptedException, TimeoutException {

		Message sendFailedMessage = mock(Message.class);
		doReturn(sendFailedMessage).when(messageFactory).createSendFailedResponseMessage(anyInt(), any(), any());

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		AtomicReference<Message> callbackMessage = new AtomicReference<>();
		remoting.asyncCall(requestMessage, connection, 1000, (msg) -> {
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
		doReturn(sendFailedMessage).when(messageFactory).createSendFailedResponseMessage(anyInt(), any(), any());

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		AtomicReference<Message> callbackMessage = new AtomicReference<>();
		remoting.asyncCall(requestMessage, connection, 1000, (msg) -> {
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

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		AtomicReference<Message> callbackMessage = new AtomicReference<>();
		remoting.asyncCall(requestMessage, connection, 1000, (msg) -> {
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

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);
		remoting.oneway(requestMessage, connection);

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

		verify(channel, times(1)).writeAndFlush(eq(requestMessage));
	}

	@Test
	public void testOnewaySendFailed1() throws InterruptedException, TimeoutException {

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		remoting.oneway(requestMessage, connection);

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

		verify(channel, times(1)).writeAndFlush(eq(requestMessage));
	}

	@Test
	public void testOnewaySendFailed2() throws InterruptedException, TimeoutException {

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel);

		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		remoting.oneway(requestMessage, connection);

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

		verify(channel, times(1)).writeAndFlush(eq(requestMessage));
	}

}
