package io.github.xinfra.lab.remoting.rpc.protocol;

import io.github.xinfra.lab.remoting.codec.MessageCodec;
import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.protocol.ProtocolId;
import io.github.xinfra.lab.remoting.rpc.message.MessageFactory;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

public class TestRpcProtocol implements RpcProtocol {

	private byte[] protocolCode = "test-rpc-protocol".getBytes(StandardCharsets.UTF_8);

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
