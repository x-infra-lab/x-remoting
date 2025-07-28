package io.github.xinfra.lab.remoting.impl.codec;

import io.github.xinfra.lab.remoting.codec.MessageCodec;

public class RpcMessageCodec implements MessageCodec {

	private final RemotingMessageEncoder remotingMessageEncoder;

	private final RemotingMessageDecoder remotingMessageDecoder;

	public RpcMessageCodec() {
		this.remotingMessageEncoder = new RemotingMessageEncoder();
		this.remotingMessageDecoder = new RemotingMessageDecoder();
	}

	@Override
	public RemotingMessageEncoder encoder() {
		return remotingMessageEncoder;
	}

	@Override
	public RemotingMessageDecoder decoder() {
		return remotingMessageDecoder;
	}

}
