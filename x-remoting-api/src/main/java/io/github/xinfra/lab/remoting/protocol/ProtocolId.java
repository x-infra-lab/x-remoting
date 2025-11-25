package io.github.xinfra.lab.remoting.protocol;

public interface ProtocolId {

	byte[] getCodes();

	default byte version() {
		return 0x01;
	}

}
