package io.github.xinfra.lab.remoting.impl.codec;

import io.github.xinfra.lab.remoting.codec.MessageCodec;

public class RemotingMessageCodec implements MessageCodec {

	private final RemotingMessageEncoder remotingMessageEncoder;

	private final RemotingMessageDecoder remotingMessageDecoder;

	public RemotingMessageCodec() {
		this.remotingMessageEncoder = new RemotingMessageEncoder();
		this.remotingMessageDecoder = new RemotingMessageDecoder();
	}

	@Override
	public RemotingMessageEncoder getEncoder() {
		return remotingMessageEncoder;
	}

	@Override
	public RemotingMessageDecoder getDecoder() {
		return remotingMessageDecoder;
	}

}
