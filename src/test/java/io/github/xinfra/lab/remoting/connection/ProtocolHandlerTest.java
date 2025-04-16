package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.Timer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ProtocolHandlerTest {

	TestProtocol testProtocol = new TestProtocol();

	@Test
	public void testProtocolHandler() {
		ProtocolHandler protocolHandler = new ProtocolHandler();

		EmbeddedChannel channel = new EmbeddedChannel();
		channel.pipeline().addLast(protocolHandler);

		MessageHandler messageHandler = new MessageHandler() {

			@Override
			public void close() throws IOException {

			}

			@Override
			public ExecutorService executor() {
				return null;
			}

			@Override
			public void handleMessage(MessageHandlerContext remotingContext, Message msg) {

			}

			@Override
			public Timer timer() {
				return null;
			}
		};

		MessageHandler spyMessageHandler = spy(messageHandler);
		testProtocol.setTestMessageHandler(spyMessageHandler);
		new Connection(testProtocol, channel);

		Object object = new Object();
		channel.writeInbound(object);
		verify(spyMessageHandler, times(0)).handleMessage(any(), any());
		Object inboundMessage = channel.inboundMessages().poll();
		Assertions.assertTrue(object == inboundMessage);

		Message message = mock(Message.class);
		channel.writeInbound(message);
		verify(spyMessageHandler, times(1)).handleMessage(any(), any());
	}

}
