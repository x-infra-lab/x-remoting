package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.rpc.client.IDGenerator;
import io.github.xinfra.lab.remoting.rpc.client.InFlightRequests;
import io.github.xinfra.lab.remoting.rpc.client.InvokeFuture;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.rpc.protocol.RemotingProtocol;
import io.github.xinfra.lab.remoting.rpc.handler.EchoRequest;
import io.github.xinfra.lab.remoting.rpc.handler.EchoRequestHandler;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static io.github.xinfra.lab.remoting.rpc.handler.RequestApis.echoPath;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Slf4j
public class RpcMessageDispatcherTest {

	private RemotingProtocol protocol;

	private RemotingRequestMessageHandler requestMessageHandler;

	private ExecutorService executorService;

	private Timer timer;

	@BeforeEach
	public void beforeEach() {
		requestMessageHandler = new RemotingRequestMessageHandler();
		protocol = new RemotingProtocol(requestMessageHandler);
		executorService = Executors.newSingleThreadExecutor();
		timer = new HashedWheelTimer();
	}

	@AfterEach
	public void afterEach() {
		executorService.shutdown();
		timer.stop();
	}

	@Test
	public void testHandleRequest() throws SerializeException, InterruptedException, TimeoutException {
		String content = "this is rpc content";
		EchoRequest echoRequest = new EchoRequest(content);

		Integer requestId = new IDGenerator().nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hessian);
		requestMessage.setPath(echoPath);
		requestMessage.setHeaders(new DefaultMessageHeaders());
		requestMessage.setBody(new RemotingMessageBody(echoRequest));
		requestMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		echoRequestHandler = spy(echoRequestHandler);
		requestMessageHandler.register(echoPath, echoRequestHandler);

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = spy(new EmbeddedChannel());
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(channel).writeAndFlush(any());
		new Connection(protocol, channel, executorService, timer);

		messageHandler.handleMessage(context, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(channel, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(echoRequestHandler, times(1)).handle(any(), any());
		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingResponseMessage>() {
			@Override
			public boolean matches(RemotingResponseMessage responseMessage) {
				if (responseMessage.getResponseStatus() != ResponseStatus.OK) {
					return false;
				}
				if (!responseMessage.getBody().getBodyValue().equals("echo:" + content)) {
					return false;
				}
				return true;
			}
		}));
	}

	@Test
	public void testHandleRequestWithCustomExecutor()
			throws SerializeException, InterruptedException, TimeoutException {
		final AtomicBoolean threadPoolExecuted = new AtomicBoolean(false);
		ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r) {
					@Override
					public void run() {
						threadPoolExecuted.set(true);
						super.run();
					}
				};
			}
		});

		String content = "this is rpc content";
		EchoRequest echoRequest = new EchoRequest(content);

		Integer requestId = new IDGenerator().nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hessian);
		requestMessage.setPath(echoPath);
		requestMessage.setHeaders(new DefaultMessageHeaders());
		requestMessage.setBody(new RemotingMessageBody(echoRequest));
		requestMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		echoRequestHandler.setExecutor(executor);
		echoRequestHandler = spy(echoRequestHandler);
		requestMessageHandler.register(echoPath, echoRequestHandler);

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = spy(new EmbeddedChannel());
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(channel).writeAndFlush(any());
		new Connection(protocol, channel, executorService, timer);

		messageHandler.handleMessage(context, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(channel, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(echoRequestHandler, times(1)).handle(any(), any());
		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingResponseMessage>() {
			@Override
			public boolean matches(RemotingResponseMessage responseMessage) {
				if (responseMessage.getResponseStatus() != ResponseStatus.OK) {
					return false;
				}
				if (!responseMessage.getBody().getBodyValue().equals("echo:" + content)) {
					return false;
				}
				return true;
			}
		}));
		Assertions.assertTrue(threadPoolExecuted.get());
	}

	@Test
	public void testHandleRequestDeserializeFailed()
			throws SerializeException, InterruptedException, TimeoutException, DeserializeException {
		String content = "this is rpc content";
		EchoRequest echoRequest = new EchoRequest(content);

		Integer requestId = new IDGenerator().nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hessian);

		requestMessage.setPath(echoPath);
		requestMessage.setBody(new RemotingMessageBody(echoRequest));
		requestMessage.serialize();

		requestMessage = spy(requestMessage);
		doThrow(new DeserializeException("deserialize exception")).when(requestMessage).deserialize();

		MessageHandler messageHandler = protocol.getMessageHandler();
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		echoRequestHandler = spy(echoRequestHandler);
		requestMessageHandler.register(echoPath, echoRequestHandler);

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = spy(new EmbeddedChannel());
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(channel).writeAndFlush(any());
		new Connection(protocol, channel, executorService, timer);

		messageHandler.handleMessage(context, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(channel, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(echoRequestHandler, times(0)).handle(any(), any());
		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingResponseMessage>() {
			@Override
			public boolean matches(RemotingResponseMessage responseMessage) {
				if (responseMessage.getResponseStatus() != ResponseStatus.DeserializeException) {
					return false;
				}
				return true;
			}
		}));
	}

	@Test
	public void testRequestHandlerNotFound()
			throws SerializeException, InterruptedException, TimeoutException, DeserializeException {
		String content = "this is rpc content";
		EchoRequest echoRequest = new EchoRequest(content);

		Integer requestId = new IDGenerator().nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hessian);

		requestMessage.setPath(echoPath + "not found");
		requestMessage.setBody(new RemotingMessageBody(echoRequest));
		requestMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		echoRequestHandler = spy(echoRequestHandler);
		requestMessageHandler.register(echoPath, echoRequestHandler);

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = spy(new EmbeddedChannel());
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(channel).writeAndFlush(any());
		new Connection(protocol, channel, executorService, timer);

		messageHandler.handleMessage(context, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(channel, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(echoRequestHandler, times(0)).handle(any(), any());
		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingResponseMessage>() {
			@Override
			public boolean matches(RemotingResponseMessage responseMessage) {
				if (responseMessage.getResponseStatus() != ResponseStatus.NotFound) {
					return false;
				}
				return true;
			}
		}));
	}

	@Test
	public void testRequestHandlerException()
			throws SerializeException, InterruptedException, TimeoutException, DeserializeException {
		String content = "this is rpc content";
		EchoRequest echoRequest = new EchoRequest(content);

		Integer requestId = new IDGenerator().nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hessian);

		requestMessage.setPath(echoPath);
		requestMessage.setBody(new RemotingMessageBody(echoRequest));
		requestMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		echoRequestHandler = spy(echoRequestHandler);
		requestMessageHandler.register(echoPath, echoRequestHandler);
		doThrow(new IllegalArgumentException("test exception")).when(echoRequestHandler).handleRequest(any());

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = spy(new EmbeddedChannel());
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(channel).writeAndFlush(any());
		new Connection(protocol, channel, executorService, timer);

		messageHandler.handleMessage(context, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(channel, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(echoRequestHandler, times(1)).handle(any(), any());
		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingResponseMessage>() {
			@Override
			public boolean matches(RemotingResponseMessage responseMessage) {
				if (responseMessage.getResponseStatus() != ResponseStatus.Error) {
					return false;
				}
				if (!(responseMessage.getBody().getBodyValue() instanceof IllegalArgumentException)) {
					return false;
				}
				return true;
			}
		}));
	}

	@Test
	public void testHandleHeartbeatRequest() throws SerializeException, InterruptedException, TimeoutException {
		Integer requestId = new IDGenerator().nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.heartbeatRequest,
				SerializationType.Hessian);

		requestMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = spy(new EmbeddedChannel());
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(channel).writeAndFlush(any());
		new Connection(protocol, channel, executorService, timer);

		messageHandler.handleMessage(context, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(channel, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingResponseMessage>() {
			@Override
			public boolean matches(RemotingResponseMessage responseMessage) {
				if (responseMessage.getResponseStatus() != ResponseStatus.OK) {
					return false;
				}
				return true;
			}
		}));
	}

	@Test
	public void testHandleResponse() throws SerializeException, InterruptedException, TimeoutException {
		String content = "this is rpc content";
		Integer requestId = new IDGenerator().nextRequestId();
		RemotingResponseMessage responseMessage = new RemotingResponseMessage(requestId, SerializationType.Hessian,
				ResponseStatus.OK);
		responseMessage.setBody(new RemotingMessageBody(content));
		responseMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		Connection connection = new Connection(protocol, channel, executorService, timer);
		channel.attr(CONNECTION).set(connection);

		InvokeFuture future = mock(InvokeFuture.class);
		doReturn(requestId).when(future).getRequestId();
		InFlightRequests.getOrCreate(connection).add(future);

		messageHandler.handleMessage(context, responseMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(future, atLeastOnce()).executeCallBack(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(future, times(1)).cancelTimeout();
		verify(future, times(1)).complete(eq(responseMessage));
		verify(future, times(1)).executeCallBack(any());

	}

	@Test
	public void testHandleResponseCallbackException()
			throws SerializeException, InterruptedException, TimeoutException {
		String content = "this is rpc content";
		Integer requestId = new IDGenerator().nextRequestId();
		RemotingResponseMessage responseMessage = new RemotingResponseMessage(requestId, SerializationType.Hessian,
				ResponseStatus.OK);
		responseMessage.setBody(new RemotingMessageBody(content));
		responseMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		Connection connection = new Connection(protocol, channel, executorService, timer);
		channel.attr(CONNECTION).set(connection);

		InvokeFuture future = mock(InvokeFuture.class);
		doReturn(requestId).when(future).getRequestId();
		InFlightRequests.getOrCreate(connection).add(future);

		doThrow(new RuntimeException("testHandleResponseCallbackException")).when(future).executeCallBack(any());

		messageHandler.handleMessage(context, responseMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(future, atLeastOnce()).executeCallBack(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(future, times(1)).cancelTimeout();
		verify(future, times(1)).complete(eq(responseMessage));
		verify(future, times(1)).executeCallBack(any());

	}

	@Test
	public void testRegisterRequestHandler() {
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		requestMessageHandler.register(echoPath, echoRequestHandler);

		Assertions.assertEquals(echoRequestHandler, requestMessageHandler.lookup(echoPath));
	}

}
