package io.github.xinfra.lab.remoting.impl;

import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.impl.codec.RpcMessageCodec;
import io.github.xinfra.lab.remoting.impl.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.impl.message.RpcMessageHandler;

/**
 * remoting protocol definition:
 * <p>
 * request definition:
 * <p>
 * ｜protocol:bytes|protocol-version:byte|message-type:byte|request-id:int|serialization-type:byte|path-length:short|header-length:short|body-length:int|path|header|content|
 * <p>
 * response definition:
 * <p>
 * ｜protocol:bytes|protocol-version:byte|message-type:byte|request-id:int|serialization-type:byte|status:short|header-length:short|body-length:int|header|content|
 */
public class RemotingProtocol implements Protocol {

	public static int RESPONSE_HEADER_BYTES = 16;

	public static int REQUEST_HEADER_BYTES = 16;

	private final RpcMessageCodec rpcMessageCodec;

	private final RpcMessageHandler rpcMessageHandler;

	private final RpcMessageFactory rpcMessageFactory;

	public RemotingProtocol() {
		this.rpcMessageCodec = new RpcMessageCodec();
		this.rpcMessageHandler = new RpcMessageHandler();
		this.rpcMessageFactory = new RpcMessageFactory();
	}

	@Override
	public RemotingProtocolIdentifier protocolCode() {
		return RemotingProtocolIdentifier.INSTANCE;
	}

	@Override
	public RpcMessageCodec messageCodec() {
		return rpcMessageCodec;
	}

	@Override
	public RpcMessageHandler messageHandler() {
		return this.rpcMessageHandler;
	}

	@Override
	public RpcMessageFactory messageFactory() {
		return this.rpcMessageFactory;
	}

}
