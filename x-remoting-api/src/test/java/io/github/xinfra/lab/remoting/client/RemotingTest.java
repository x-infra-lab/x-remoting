package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.MessageTypeHandler;
import io.github.xinfra.lab.remoting.message.RequestMessage;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
import static org.mockito.Mockito.when;

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
		timer = new HashedWheelTimer();

		// set messageFactory
		messageFactory = mock(MessageFactory.class);
		testProtocol.setMessageFactory(messageFactory);

		requestId = IDGenerator.nextRequestId();
		remoting = new Remoting() {
		};
	}

	@AfterEach
	public void afterEach() throws IOException {
		executor.shutdownNow();
		timer.stop();
	}

	@Test
	public void testSyncCall() throws InterruptedException {
		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();

		Channel channel = new EmbeddedChannel();
		Connection connection = new Connection(testProtocol, channel, executor, timer);

		ResponseMessage mockResponseMessage = mock(ResponseMessage.class);
		// complete invokeFuture
		executor.submit(() -> {
			try {
				Wait.untilIsTrue(() -> {
					InvokeFuture invokeFuture = connection.removeInvokeFuture(requestId);
					if (invokeFuture != null) {
						invokeFuture.complete(mockResponseMessage);
						return true;
					}
					return false;
				}, 30, 100);
			}
			catch (InterruptedException | TimeoutException e) {
				throw new RuntimeException(e);
			}
		});

		ResponseMessage responseMessage = remoting.syncCall(requestMessage, connection, 1000);

		Assertions.assertTrue(mockResponseMessage == responseMessage);
	}

	@Test
	public void testSyncCallSendFailed1() throws InterruptedException {
		ResponseMessage mockSendFailedresponseMessage = mock(ResponseMessage.class);
		when(messageFactory.createResponse(anyInt(), eq(ResponseStatus.SendFailed), any()))
			.thenReturn(mockSendFailedresponseMessage);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel, executor, timer);

		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		ResponseMessage responseMessage = remoting.syncCall(requestMessage, connection, 1000);
		Assertions.assertTrue(responseMessage == mockSendFailedresponseMessage);
	}

	@Test
	public void testSyncCallSendFailed2() throws InterruptedException {
		ResponseMessage mockSendFailedresponseMessage = mock(ResponseMessage.class);
		when(messageFactory.createResponse(anyInt(), eq(ResponseStatus.SendFailed), any()))
			.thenReturn(mockSendFailedresponseMessage);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel, executor, timer);

		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		ResponseMessage responseMessage = remoting.syncCall(requestMessage, connection, 1000);
		Assertions.assertTrue(responseMessage == mockSendFailedresponseMessage);
	}

	@Test
	public void testSyncCallTimeout() throws InterruptedException {
		ResponseMessage mockTimeoutresponseMessage = mock(ResponseMessage.class);
		when(messageFactory.createResponse(anyInt(), eq(ResponseStatus.Timeout)))
			.thenReturn(mockTimeoutresponseMessage);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		Connection connection = new Connection(testProtocol, channel, executor, timer);
		ResponseMessage responseMessage = remoting.syncCall(requestMessage, connection, 100);
		Assertions.assertTrue(responseMessage == mockTimeoutresponseMessage);
	}

	@Test
	public void testAsyncCall() throws InterruptedException, TimeoutException {
		ResponseMessage responseMessage = mock(ResponseMessage.class);
		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		Connection connection = new Connection(testProtocol, channel, executor, timer);
		InvokeFuture invokeFuture = remoting.asyncCall(requestMessage, connection, 1000);

		// complete invokeFuture
		Wait.untilIsTrue(() -> {
			InvokeFuture future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.cancelTimeout();
				future.complete(responseMessage);
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(invokeFuture.get() == responseMessage);

	}

	@Test
	public void testAsyncCallSendFailed1() throws InterruptedException {
		ResponseMessage mockSendFailedresponseMessage = mock(ResponseMessage.class);
		when(messageFactory.createResponse(anyInt(), eq(ResponseStatus.SendFailed), any()))
			.thenReturn(mockSendFailedresponseMessage);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel, executor, timer);
		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		InvokeFuture invokeFuture = remoting.asyncCall(requestMessage, connection, 1000);
		Assertions.assertTrue(invokeFuture.get() == mockSendFailedresponseMessage);
	}

	@Test
	public void testAsyncCallSendFailed2() throws InterruptedException {
		ResponseMessage mockSendFailedresponseMessage = mock(ResponseMessage.class);
		when(messageFactory.createResponse(anyInt(), eq(ResponseStatus.SendFailed), any()))
			.thenReturn(mockSendFailedresponseMessage);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel, executor, timer);
		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		InvokeFuture invokeFuture = remoting.asyncCall(requestMessage, connection, 1000);
		Assertions.assertTrue(invokeFuture.get() == mockSendFailedresponseMessage);
	}

	@Test
	public void testAsyncCallTimeout() throws InterruptedException {
		ResponseMessage mockTimeoutresponseMessage = mock(ResponseMessage.class);
		when(messageFactory.createResponse(anyInt(), eq(ResponseStatus.Timeout)))
			.thenReturn(mockTimeoutresponseMessage);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();

		Connection connection = new Connection(testProtocol, channel, executor, timer);
		InvokeFuture invokeFuture = remoting.asyncCall(requestMessage, connection, 100);
		Assertions.assertTrue(invokeFuture.get() == mockTimeoutresponseMessage);
	}

	@Test
	public void testAsyncCallWithCallback() throws InterruptedException, TimeoutException {
		ResponseMessage responseMessage = mock(ResponseMessage.class);
		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		Connection connection = new Connection(testProtocol, channel, executor, timer);

		AtomicReference<Message> callbackMessage = new AtomicReference<>();
		remoting.asyncCall(requestMessage, connection, 1000, (msg) -> {
			callbackMessage.set(msg);
		});

		// complete invokeFuture
		Wait.untilIsTrue(() -> {
			InvokeFuture future = connection.removeInvokeFuture(requestId);
			if (future != null) {
				future.cancelTimeout();
				future.complete(responseMessage);
				future.asyncExecuteCallBack(connection.getExecutor());
				return true;
			}
			return false;
		}, 30, 100);

		// wait callback execute
		Wait.untilIsTrue(() -> {
			if (callbackMessage.get() != null) {
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(callbackMessage.get() == responseMessage);
	}

	@Test
	public void testAsyncCallWithCallbackSendFailed1() throws InterruptedException, TimeoutException {
		ResponseMessage mockSendFailedresponseMessage = mock(ResponseMessage.class);
		when(messageFactory.createResponse(anyInt(), eq(ResponseStatus.SendFailed), any()))
			.thenReturn(mockSendFailedresponseMessage);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel, executor, timer);

		doThrow(new RuntimeException("network error")).when(channel).writeAndFlush(any());

		AtomicReference<ResponseMessage> callbackMessage = new AtomicReference<>();
		remoting.asyncCall(requestMessage, connection, 1000, (msg) -> {
			callbackMessage.set(msg);
		});

		Wait.untilIsTrue(() -> {
			if (callbackMessage.get() != null) {
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(callbackMessage.get() == mockSendFailedresponseMessage);
	}

	@Test
	public void testAsyncCallWithCallbackSendFailed2() throws InterruptedException, TimeoutException {
		ResponseMessage mockSendFailedresponseMessage = mock(ResponseMessage.class);
		when(messageFactory.createResponse(anyInt(), eq(ResponseStatus.SendFailed), any()))
			.thenReturn(mockSendFailedresponseMessage);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel, executor, timer);

		doReturn(channel.newFailedFuture(new RuntimeException("network error"))).when(channel).writeAndFlush(any());

		AtomicReference<ResponseMessage> callbackMessage = new AtomicReference<>();
		remoting.asyncCall(requestMessage, connection, 1000, (msg) -> {
			callbackMessage.set(msg);
		});

		Wait.untilIsTrue(() -> {
			if (callbackMessage.get() != null) {
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(callbackMessage.get() == mockSendFailedresponseMessage);
	}

	@Test
	public void testAsyncCallWithCallbackTimeout() throws InterruptedException, TimeoutException {
		ResponseMessage mockTimeoutresponseMessage = mock(ResponseMessage.class);
		when(messageFactory.createResponse(anyInt(), eq(ResponseStatus.Timeout)))
			.thenReturn(mockTimeoutresponseMessage);

		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		Connection connection = new Connection(testProtocol, channel, executor, timer);

		AtomicReference<ResponseMessage> callbackMessage = new AtomicReference<>();
		remoting.asyncCall(requestMessage, connection, 100, (msg) -> {
			callbackMessage.set(msg);
		});

		Wait.untilIsTrue(() -> {
			if (callbackMessage.get() != null) {
				return true;
			}
			return false;
		}, 30, 100);

		Assertions.assertTrue(callbackMessage.get() == mockTimeoutresponseMessage);
	}

	@Test
	public void testOneway() throws InterruptedException, TimeoutException {
		RequestMessage requestMessage = mock(RequestMessage.class);
		doReturn(requestId).when(requestMessage).id();
		Channel channel = new EmbeddedChannel();
		channel = spy(channel);
		Connection connection = new Connection(testProtocol, channel, executor, timer);
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
		Connection connection = new Connection(testProtocol, channel, executor, timer);

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
		Connection connection = new Connection(testProtocol, channel, executor, timer);

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
