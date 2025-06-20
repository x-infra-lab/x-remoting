package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageCodec;
import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.Heartbeater;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import lombok.Setter;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class TestProtocol implements Protocol {

	@Setter
	private byte[] protocolCode = "test".getBytes(StandardCharsets.UTF_8);

	@Setter
	private MessageEncoder testMessageEncoder;

	@Setter
	private MessageDecoder testMessageDecoder;

	@Setter
	private MessageHandler testMessageHandler;

	@Setter
	private MessageFactory testMessageFactory;

	@Override
	public ProtocolCode protocolCode() {
		return null;
	}

	@Override
	public MessageCodec messageCodec() {
		return new MessageCodec() {
			@Override
			public MessageEncoder encoder() {
				return testMessageEncoder;
			}

			@Override
			public MessageDecoder decoder() {
				return testMessageDecoder;
			}
		};
	}

	@Override
	public MessageHandler messageHandler() {
		return testMessageHandler;
	}

	@Override
	public MessageFactory messageFactory() {
		return testMessageFactory;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

}
