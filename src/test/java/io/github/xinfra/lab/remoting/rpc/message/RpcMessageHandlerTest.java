package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.rpc.RpcProtocol;
import io.github.xinfra.lab.remoting.rpc.exception.RpcServerException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
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

public class RpcMessageHandlerTest {

	static class EchoProcessor implements UserProcessor<String> {

		@Override
		public String interest() {
			return String.class.getName();
		}

		@Override
		public Object handRequest(String request) {
			// echo request
			return "echo:" + request;
		}

	}

	private RpcProtocol protocol;

	@BeforeEach
	public void beforeEach() {
		protocol = new RpcProtocol();
	}

	@AfterEach
	public void afterEach() throws IOException {
		protocol.close();
	}

	@Test
	public void testHandleRequest() throws SerializeException, InterruptedException, TimeoutException {
		// build a requestMessage
		String content = "this is rpc content";
		String contentType = content.getClass().getName();
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcRequestMessage requestMessage = new RpcRequestMessage(requestId);
		requestMessage.setHeader(header);
		requestMessage.setContent(content);
		requestMessage.setContentType(contentType);
		requestMessage.serialize();
		requestMessage.setHeader(null);
		requestMessage.setContentType(null);
		requestMessage.setContent(null);

		MessageHandler messageHandler = protocol.messageHandler();
		EchoProcessor echoProcessor = new EchoProcessor();
		echoProcessor = spy(echoProcessor);
		messageHandler.registerUserProcessor(echoProcessor);

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(context).writeAndFlush(any());
		new Connection(protocol, channel);

		MessageHandlerContext messageHandlerContext = new MessageHandlerContext(context);
		messageHandler.handleMessage(messageHandlerContext, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(context, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(echoProcessor, times(1)).handRequest(eq(content));
		// verify response
		verify(context, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RpcResponseMessage>() {
			@Override
			public boolean matches(RpcResponseMessage argument) {
				if (argument.getStatus() != ResponseStatus.SUCCESS.getCode()) {
					return false;
				}
				if (!Objects.equals(argument.getContentType(), String.class.getName())) {
					return false;
				}
				if (!Objects.equals(argument.getContent(), "echo:" + content)) {
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
		String contentType = content.getClass().getName();
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcRequestMessage requestMessage = new RpcRequestMessage(requestId);
		requestMessage.setHeader(header);
		requestMessage.setContent(content);
		requestMessage.setContentType(contentType);
		requestMessage.serialize();
		requestMessage.setHeader(null);
		requestMessage.setContentType(null);
		requestMessage.setContent(null);

		requestMessage = spy(requestMessage);
		doThrow(new RuntimeException("deserialize exception")).when(requestMessage).deserialize(any());

		MessageHandler messageHandler = protocol.messageHandler();
		EchoProcessor echoProcessor = new EchoProcessor();
		echoProcessor = spy(echoProcessor);
		messageHandler.registerUserProcessor(echoProcessor);

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(context).writeAndFlush(any());
		new Connection(protocol, channel);

		MessageHandlerContext messageHandlerContext = new MessageHandlerContext(context);
		messageHandler.handleMessage(messageHandlerContext, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(context, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(echoProcessor, times(0)).handRequest(eq(content));
		// verify response
		verify(context, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RpcResponseMessage>() {
			@Override
			public boolean matches(RpcResponseMessage argument) {
				if (argument.getStatus() != ResponseStatus.SERVER_DESERIAL_EXCEPTION.getCode()) {
					return false;
				}
				if (argument.getCause() != null) {
					return false;
				}
				if (!Objects.equals(argument.getContentType(), RpcServerException.class.getName())) {
					return false;
				}
				if (!(argument.getContent() instanceof RpcServerException)) {
					return false;
				}
				return true;
			}
		}));
	}

	@Test
	public void testHandleRequestUserProcessorNotFind()
			throws SerializeException, InterruptedException, TimeoutException, DeserializeException {
		// build a requestMessage
		String content = "this is rpc content";
		String contentType = content.getClass().getName();
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcRequestMessage requestMessage = new RpcRequestMessage(requestId);
		requestMessage.setHeader(header);
		requestMessage.setContent(content);
		requestMessage.setContentType(contentType);
		requestMessage.serialize();
		requestMessage.setHeader(null);
		requestMessage.setContentType(null);
		requestMessage.setContent(null);

		MessageHandler messageHandler = protocol.messageHandler();
		EchoProcessor echoProcessor = new EchoProcessor();
		echoProcessor = spy(echoProcessor);
		messageHandler.registerUserProcessor(echoProcessor);

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(context).writeAndFlush(any());

		// mock
		protocol = spy(protocol);
		messageHandler = spy(messageHandler);
		doReturn(messageHandler).when(protocol).messageHandler();
		doReturn(null).when(messageHandler).userProcessor(any());

		new Connection(protocol, channel);

		MessageHandlerContext messageHandlerContext = new MessageHandlerContext(context);
		messageHandler.handleMessage(messageHandlerContext, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(context, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(echoProcessor, times(0)).handRequest(eq(content));
		// verify response
		verify(context, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RpcResponseMessage>() {
			@Override
			public boolean matches(RpcResponseMessage argument) {
				if (argument.getStatus() != ResponseStatus.SERVER_EXCEPTION.getCode()) {
					return false;
				}
				if (argument.getCause() != null) {
					return false;
				}
				if (!Objects.equals(argument.getContentType(), RpcServerException.class.getName())) {
					return false;
				}
				if (!(argument.getContent() instanceof RpcServerException)) {
					return false;
				}
				return true;
			}
		}));
	}

	@Test
	public void testHandleRequestUserProcessorException()
			throws SerializeException, InterruptedException, TimeoutException, DeserializeException {
		// build a requestMessage
		String content = "this is rpc content";
		String contentType = content.getClass().getName();
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcRequestMessage requestMessage = new RpcRequestMessage(requestId);
		requestMessage.setHeader(header);
		requestMessage.setContent(content);
		requestMessage.setContentType(contentType);
		requestMessage.serialize();
		requestMessage.setHeader(null);
		requestMessage.setContentType(null);
		requestMessage.setContent(null);

		MessageHandler messageHandler = protocol.messageHandler();
		EchoProcessor echoProcessor = new EchoProcessor();
		echoProcessor = spy(echoProcessor);

		// mock
		doThrow(new RuntimeException("user process exception")).when(echoProcessor).handRequest(any());

		messageHandler.registerUserProcessor(echoProcessor);

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(context).writeAndFlush(any());
		new Connection(protocol, channel);

		MessageHandlerContext messageHandlerContext = new MessageHandlerContext(context);
		messageHandler.handleMessage(messageHandlerContext, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(context, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		verify(echoProcessor, times(1)).handRequest(eq(content));
		// verify response
		verify(context, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RpcResponseMessage>() {
			@Override
			public boolean matches(RpcResponseMessage argument) {
				if (argument.getStatus() != ResponseStatus.SERVER_EXCEPTION.getCode()) {
					return false;
				}
				if (argument.getCause() != null) {
					return false;
				}
				if (!Objects.equals(argument.getContentType(), RpcServerException.class.getName())) {
					return false;
				}
				if (!(argument.getContent() instanceof RpcServerException)) {
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
		RpcHeartbeatRequestMessage requestMessage = new RpcHeartbeatRequestMessage(requestId);
		requestMessage.serialize();

		MessageHandler messageHandler = protocol.messageHandler();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		doReturn(channel.newSucceededFuture()).when(context).writeAndFlush(any());
		new Connection(protocol, channel);

		MessageHandlerContext messageHandlerContext = new MessageHandlerContext(context);
		messageHandler.handleMessage(messageHandlerContext, requestMessage);

		Wait.untilIsTrue(() -> {
			try {
				verify(context, atLeastOnce()).writeAndFlush(any());
				return true;
			}
			catch (Throwable t) {
				return false;
			}
		}, 30, 100);

		// verify response
		verify(context, times(1)).writeAndFlush(argThat(new ArgumentMatcher<RpcResponseMessage>() {
			@Override
			public boolean matches(RpcResponseMessage argument) {
				if (argument.getStatus() != ResponseStatus.SUCCESS.getCode()) {
					return false;
				}
				if (!Objects.equals(argument.getContentType(), null)) {
					return false;
				}
				if (!Objects.equals(argument.getContent(), null)) {
					return false;
				}
				return true;
			}
		}));
	}

	@Test
	public void testHandleResponse() throws SerializeException, InterruptedException, TimeoutException {
		// build a response
		String content = "this is rpc content";
		String contentType = content.getClass().getName();
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcResponseMessage responseMessage = new RpcResponseMessage(requestId);
		responseMessage.setHeader(header);
		responseMessage.setContent(content);
		responseMessage.setContentType(contentType);
		responseMessage.setStatus(ResponseStatus.SUCCESS.getCode());
		responseMessage.serialize();
		responseMessage.setContentType(null);
		responseMessage.setHeader(null);
		responseMessage.setContent(null);

		MessageHandler messageHandler = protocol.messageHandler();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		Connection connection = new Connection(protocol, channel);
		connection = spy(connection);
		channel.attr(CONNECTION).set(connection);

		InvokeFuture future = mock(InvokeFuture.class);
		doReturn(future).when(connection).removeInvokeFuture(eq(requestId));

		MessageHandlerContext messageHandlerContext = new MessageHandlerContext(context);
		messageHandler.handleMessage(messageHandlerContext, responseMessage);

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
		String contentType = content.getClass().getName();
		RpcMessageHeader header = new RpcMessageHeader();
		header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
		Integer requestId = IDGenerator.nextRequestId();
		RpcResponseMessage responseMessage = new RpcResponseMessage(requestId);
		responseMessage.setHeader(header);
		responseMessage.setContent(content);
		responseMessage.setContentType(contentType);
		responseMessage.setStatus(ResponseStatus.SUCCESS.getCode());
		responseMessage.serialize();
		responseMessage.setContentType(null);
		responseMessage.setHeader(null);
		responseMessage.setContent(null);

		MessageHandler messageHandler = protocol.messageHandler();

		ChannelHandlerContext context = mock(ChannelHandlerContext.class);
		EmbeddedChannel channel = new EmbeddedChannel();
		doReturn(channel).when(context).channel();
		Connection connection = new Connection(protocol, channel);
		connection = spy(connection);
		channel.attr(CONNECTION).set(connection);

		InvokeFuture future = mock(InvokeFuture.class);
		doReturn(future).when(connection).removeInvokeFuture(eq(requestId));

		doThrow(new RuntimeException("testHandleResponseCallbackException")).when(future).executeCallBack();

		MessageHandlerContext messageHandlerContext = new MessageHandlerContext(context);
		messageHandler.handleMessage(messageHandlerContext, responseMessage);

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
		MessageHandler messageHandler = protocol.messageHandler();
		EchoProcessor echoProcessor = new EchoProcessor();
		echoProcessor = spy(echoProcessor);

		messageHandler.registerUserProcessor(echoProcessor);

		EchoProcessor finalEchoProcessor = echoProcessor;
		Assertions.assertThrows(RuntimeException.class, () -> {
			messageHandler.registerUserProcessor(finalEchoProcessor);
		});

		Assertions.assertEquals(echoProcessor, messageHandler.userProcessor(echoProcessor.interest()));
	}

}
