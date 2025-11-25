package io.github.xinfra.lab.remoting.impl;

import io.github.xinfra.lab.remoting.protocol.ProtocolIdentifier;

import java.nio.charset.StandardCharsets;

public enum RemotingProtocolIdentifier implements ProtocolIdentifier {

	INSTANCE;

	public static final byte[] PROTOCOL_CODE = "x".getBytes(StandardCharsets.UTF_8);

	@Override
	public byte[] code() {
		return PROTOCOL_CODE;
	}

}
