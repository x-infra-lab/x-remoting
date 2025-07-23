package io.github.xinfra.lab.remoting.rpc.codec;

import io.github.xinfra.lab.remoting.codec.MessageCodec;

public class RpcMessageCodec implements MessageCodec {

	private final RpcMessageEncoder rpcMessageEncoder;

	private final RpcMessageDecoder rpcMessageDecoder;

	public RpcMessageCodec() {
		this.rpcMessageEncoder = new RpcMessageEncoder();
		this.rpcMessageDecoder = new RpcMessageDecoder();
	}

	@Override
	public RpcMessageEncoder encoder() {
		return rpcMessageEncoder;
	}

	@Override
	public RpcMessageDecoder decoder() {
		return rpcMessageDecoder;
	}

}
