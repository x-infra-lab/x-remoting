package io.github.xinfra.lab.remoting.impl;

import io.github.xinfra.lab.remoting.protocol.ProtocolId;

import java.nio.charset.StandardCharsets;

public enum RemotingProtocolId implements ProtocolId {

	INSTANCE;

	public static final byte[] PROTOCOL_CODE = "x".getBytes(StandardCharsets.UTF_8);

	@Override
	public byte[] getCodes() {
		return PROTOCOL_CODE;
	}

}
