package io.github.xinfra.lab.remoting.impl;

import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.impl.codec.RpcMessageCodec;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageFactory;
import io.github.xinfra.lab.remoting.impl.message.RemotingMessageHandler;

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

	private final RemotingMessageHandler remotingMessageHandler;

	private final RemotingMessageFactory remotingMessageFactory;

	public RemotingProtocol() {
		this.rpcMessageCodec = new RpcMessageCodec();
		this.remotingMessageHandler = new RemotingMessageHandler();
		this.remotingMessageFactory = new RemotingMessageFactory();
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
	public RemotingMessageHandler messageHandler() {
		return this.remotingMessageHandler;
	}

	@Override
	public RemotingMessageFactory messageFactory() {
		return this.remotingMessageFactory;
	}

}
