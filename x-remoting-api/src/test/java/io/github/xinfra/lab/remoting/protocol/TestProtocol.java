package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageCodec;
import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

public class TestProtocol implements Protocol {

	private byte[] protocolCode = "test-protocol".getBytes(StandardCharsets.UTF_8);

	@Setter
	private MessageEncoder messageEncoder;

	@Setter
	private MessageDecoder messageDecoder;

	@Setter
	private MessageHandler messageHandler;

	@Setter
	private MessageFactory messageFactory;

	@Override
	public ProtocolIdentifier protocolCode() {
		return new ProtocolIdentifier() {
			@Override
			public byte[] code() {
				return protocolCode;
			}
		};
	}

	@Override
	public MessageCodec messageCodec() {
		return new MessageCodec() {
			@Override
			public MessageEncoder encoder() {
				return messageEncoder;
			}

			@Override
			public MessageDecoder decoder() {
				return messageDecoder;
			}
		};
	}

	@Override
	public MessageHandler messageHandler() {
		return messageHandler;
	}

	@Override
	public MessageFactory messageFactory() {
		return messageFactory;
	}

}
