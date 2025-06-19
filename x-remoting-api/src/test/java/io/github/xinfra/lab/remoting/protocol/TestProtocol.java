package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.heartbeat.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageFactory;
import io.netty.channel.ChannelHandlerContext;
import lombok.Setter;

import java.io.IOException;
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

	@Setter
	private HeartbeatTrigger testHeartbeatTrigger = new HeartbeatTrigger() {
		@Override
		public void triggerHeartBeat(ChannelHandlerContext ctx) {
			// do nothing
		}

		@Override
		public void setHeartbeatMaxFailCount(int failCount) {

		}

		@Override
		public void setHeartbeatTimeoutMills(int timeoutMills) {

		}
	};

	@Override
	public byte[] protocolCode() {
		return protocolCode;
	}

	@Override
	public MessageEncoder encoder() {
		return testMessageEncoder;
	}

	@Override
	public MessageDecoder decoder() {
		return testMessageDecoder;
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
	public HeartbeatTrigger heartbeatTrigger() {
		return testHeartbeatTrigger;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

}
