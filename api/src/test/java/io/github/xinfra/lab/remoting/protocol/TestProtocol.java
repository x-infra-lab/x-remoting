package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageCodec;
import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

public class TestProtocol implements Protocol {

	private byte[] protocolCode = "test-getProtocol".getBytes(StandardCharsets.UTF_8);

	@Setter
	private MessageEncoder messageEncoder;

	@Setter
	private MessageDecoder messageDecoder;

	@Setter
	private MessageHandler messageHandler;

	@Setter
	private MessageFactory messageFactory;

	@Override
	public ProtocolId getProtocolId() {
		return new ProtocolId() {
			@Override
			public byte[] getCodes() {
				return protocolCode;
			}
		};
	}

	@Override
	public MessageCodec getMessageCodec() {
		return new MessageCodec() {
			@Override
			public MessageEncoder getEncoder() {
				return messageEncoder;
			}

			@Override
			public MessageDecoder getDecoder() {
				return messageDecoder;
			}
		};
	}

	@Override
	public MessageHandler getMessageHandler() {
		return messageHandler;
	}

	@Override
	public MessageFactory getMessageFactory() {
		return messageFactory;
	}

}
