package io.github.xinfra.lab.remoting.message;

public interface RequestMessage extends Message {

	default boolean isHeartbeat() {
		return false;
	}

}
