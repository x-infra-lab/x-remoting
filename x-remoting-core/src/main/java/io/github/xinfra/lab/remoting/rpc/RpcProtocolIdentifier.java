package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.protocol.ProtocolIdentifier;

import java.nio.charset.StandardCharsets;

public enum RpcProtocolIdentifier implements ProtocolIdentifier {

	INSTANCE;

	/**
	 * short for x-remoting
	 */
	public static final byte[] PROTOCOL_CODE = "x-r".getBytes(StandardCharsets.UTF_8);

	@Override
	public byte[] code() {
		return PROTOCOL_CODE;
	}

}
