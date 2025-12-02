package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.impl.RemotingProtocol;
import io.github.xinfra.lab.remoting.impl.handler.EchoRequest;
import io.github.xinfra.lab.remoting.impl.handler.EchoRequestHandler;
import io.github.xinfra.lab.remoting.impl.handler.RequestHandlerRegistry;
import io.github.xinfra.lab.remoting.message.DefaultMessageHeaders;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
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
import java.util.concurrent.TimeoutException;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static io.github.xinfra.lab.remoting.impl.handler.RequestApis.echoApi;
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
public class RemotingRequestMessageTypeHandlerTest {

	private RemotingProtocol protocol;

	private RequestHandlerRegistry handlerRegistry;

	private ExecutorService executorService;

	private Timer timer;

	@BeforeEach
	public void beforeEach() {
		handlerRegistry = new RequestHandlerRegistry();
		protocol = new RemotingProtocol(handlerRegistry);
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
		// build a requestMessage
		String content = "this is rpc content";
		EchoRequest echoRequest = new EchoRequest(content);

		Integer requestId = IDGenerator.nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hession);
		requestMessage.setPath(echoApi.path());
		requestMessage.setHeaders(new DefaultMessageHeaders());
		requestMessage.setBody(new RemotingMessageBody(echoRequest));
		requestMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		echoRequestHandler = spy(echoRequestHandler);
		handlerRegistry.register(echoApi, echoRequestHandler);

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

		verify(echoRequestHandler, times(1)).asyncHandle(any(), any());
		// verify response
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
	public void testHandleRequestDeserializeFailed()
			throws SerializeException, InterruptedException, TimeoutException, DeserializeException {
		// build a requestMessage
		String content = "this is rpc content";
		EchoRequest echoRequest = new EchoRequest(content);

		Integer requestId = IDGenerator.nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hession);

		requestMessage.setPath(echoApi.path());
		requestMessage.setBody(new RemotingMessageBody(echoRequest));
		requestMessage.serialize();

		requestMessage = spy(requestMessage);
		doThrow(new DeserializeException("deserialize exception")).when(requestMessage).deserialize();

		MessageHandler messageHandler = protocol.getMessageHandler();
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		echoRequestHandler = spy(echoRequestHandler);
		handlerRegistry.register(echoApi, echoRequestHandler);

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

		verify(echoRequestHandler, times(0)).asyncHandle(any(), any());
		// verify response
		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingResponseMessage>() {
			@Override
			public boolean matches(RemotingResponseMessage responseMessage) {
				if (responseMessage.getResponseStatus() != ResponseStatus.DeserializeException) {
					return false;
				}
				// todo: check response body
				return true;
			}
		}));
	}

	@Test
	public void testRequestHandlerNotFound()
			throws SerializeException, InterruptedException, TimeoutException, DeserializeException {
		// build a requestMessage
		String content = "this is rpc content";
		EchoRequest echoRequest = new EchoRequest(content);

		Integer requestId = IDGenerator.nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hession);

		requestMessage.setPath(echoApi.path() + "not found");
		requestMessage.setBody(new RemotingMessageBody(echoRequest));
		requestMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		echoRequestHandler = spy(echoRequestHandler);
		handlerRegistry.register(echoApi, echoRequestHandler);

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

		verify(echoRequestHandler, times(0)).asyncHandle(any(), any());
		// verify response
		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingResponseMessage>() {
			@Override
			public boolean matches(RemotingResponseMessage responseMessage) {
				if (responseMessage.getResponseStatus() != ResponseStatus.NotFound) {
					return false;
				}
				// todo: check response body
				return true;
			}
		}));
	}

	@Test
	public void testRequestHandlerException()
			throws SerializeException, InterruptedException, TimeoutException, DeserializeException {
		// build a requestMessage
		String content = "this is rpc content";
		EchoRequest echoRequest = new EchoRequest(content);

		Integer requestId = IDGenerator.nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.request,
				SerializationType.Hession);

		requestMessage.setPath(echoApi.path());
		requestMessage.setBody(new RemotingMessageBody(echoRequest));
		requestMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		echoRequestHandler = spy(echoRequestHandler);
		handlerRegistry.register(echoApi, echoRequestHandler);
		doThrow(new IllegalArgumentException("test exception")).when(echoRequestHandler).handle(any());

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

		verify(echoRequestHandler, times(1)).asyncHandle(any(), any());
		// verify response
		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingResponseMessage>() {
			@Override
			public boolean matches(RemotingResponseMessage responseMessage) {
				if (responseMessage.getResponseStatus() != ResponseStatus.Error) {
					return false;
				}
				// todo: check response body
				if (!(responseMessage.getBody().getBodyValue() instanceof IllegalArgumentException)) {
					return false;
				}
				return true;
			}
		}));
	}

	@Test
	public void testHandleHeartbeatRequest() throws SerializeException, InterruptedException, TimeoutException {
		// build a requestMessage
		Integer requestId = IDGenerator.nextRequestId();
		RemotingRequestMessage requestMessage = new RemotingRequestMessage(requestId, MessageType.heartbeatRequest,
				SerializationType.Hession);

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

		// todo @joecqupt verify message type handler
		// verify response
		verify(channel, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RemotingResponseMessage>() {
			@Override
			public boolean matches(RemotingResponseMessage responseMessage) {
				if (responseMessage.getResponseStatus() != ResponseStatus.OK) {
					return false;
				}
				// todo: check response body
				return true;
			}
		}));
	}

	@Test
	public void testHandleResponse() throws SerializeException, InterruptedException, TimeoutException {
		// build a response
		String content = "this is rpc content";
		Integer requestId = IDGenerator.nextRequestId();
		RemotingResponseMessage responseMessage = new RemotingResponseMessage(requestId, SerializationType.Hession,
				ResponseStatus.OK);
		responseMessage.setBody(new RemotingMessageBody(content));
		responseMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		Connection connection = new Connection(protocol, channel, executorService, timer);
		connection = spy(connection);
		channel.attr(CONNECTION).set(connection);

		InvokeFuture future = mock(InvokeFuture.class);
		doReturn(future).when(connection).removeInvokeFuture(eq(requestId));

		messageHandler.handleMessage(context, responseMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(future, atLeastOnce()).executeCallBack();
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(future, times(1)).cancelTimeout();
		verify(future, times(1)).complete(eq(responseMessage));
		verify(future, times(1)).executeCallBack();

	}

	@Test
	public void testHandleResponseCallbackException()
			throws SerializeException, InterruptedException, TimeoutException {
		// build a response
		String content = "this is rpc content";
		Integer requestId = IDGenerator.nextRequestId();
		RemotingResponseMessage responseMessage = new RemotingResponseMessage(requestId, SerializationType.Hession,
				ResponseStatus.OK);
		responseMessage.setBody(new RemotingMessageBody(content));
		responseMessage.serialize();

		MessageHandler messageHandler = protocol.getMessageHandler();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		Connection connection = new Connection(protocol, channel, executorService, timer);
		connection = spy(connection);
		channel.attr(CONNECTION).set(connection);

		InvokeFuture future = mock(InvokeFuture.class);
		doReturn(future).when(connection).removeInvokeFuture(eq(requestId));

		doThrow(new RuntimeException("testHandleResponseCallbackException")).when(future).executeCallBack();

		messageHandler.handleMessage(context, responseMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(future, atLeastOnce()).executeCallBack();
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(future, times(1)).cancelTimeout();
		verify(future, times(1)).complete(eq(responseMessage));
		verify(future, times(1)).executeCallBack();

	}

	@Test
	public void testRegisterUserProcessor1() {
		EchoRequestHandler echoRequestHandler = new EchoRequestHandler();
		handlerRegistry.register(echoApi, echoRequestHandler);

		Assertions.assertEquals(echoRequestHandler, handlerRegistry.lookup(echoApi.path()));
	}

}
