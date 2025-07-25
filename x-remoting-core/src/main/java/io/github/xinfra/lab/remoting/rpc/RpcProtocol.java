package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.rpc.codec.RpcMessageCodec;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageHandler;

/**
 * x-rpc protocol definition:
 * <p>
 * request definition:
 * <p>
 * ｜protocol:bytes|protocol-version:byte|message-type:byte|request-id:int|serialization-type:byte|header-length:short|body-length:int|header|content|
 * <p>
 * response definition:
 * <p>
 * ｜protocol:bytes|protocol-version:byte|message-type:byte|request-id:int|serialization-type:byte|status:short|header-length:short|body-length:int|header|content|
 */
public class RpcProtocol implements Protocol {

	public static int RESPONSE_HEADER_BYTES = 16;

	public static int REQUEST_HEADER_BYTES = 14;

	private final RpcMessageCodec rpcMessageCodec;

	private final RpcMessageHandler rpcMessageHandler;

	private final RpcMessageFactory rpcMessageFactory;

	public RpcProtocol() {
		this.rpcMessageCodec = new RpcMessageCodec();
		this.rpcMessageHandler = new RpcMessageHandler();
		this.rpcMessageFactory = new RpcMessageFactory();
	}

	@Override
	public RpcProtocolIdentifier protocolCode() {
		return RpcProtocolIdentifier.INSTANCE;
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
