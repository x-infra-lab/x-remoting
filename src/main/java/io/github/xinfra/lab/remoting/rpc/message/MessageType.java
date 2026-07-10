package io.github.xinfra.lab.remoting.rpc.message;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public interface MessageType {

	ConcurrentMap<Byte, MessageType> REGISTRY = new ConcurrentHashMap<>();

	byte getCode();

	MessageType heartbeatRequest = create((byte) -1);

	MessageType request = create((byte) 0);

	MessageType response = create((byte) 1);

	MessageType goaway = create((byte) 2);

	static MessageType create(byte code) {
		MessageType type = () -> code;
		register(type);
		return type;
	}

	static void register(MessageType type) {
		MessageType prev = REGISTRY.putIfAbsent(type.getCode(), type);
		if (prev != null && prev != type) {
			throw new IllegalArgumentException("MessageType code " + type.getCode() + " is already registered");
		}
	}

	static MessageType valueOf(byte code) {
		MessageType type = REGISTRY.get(code);
		if (type == null) {
			throw new IllegalArgumentException("Unknown message type: " + code);
		}
		return type;
	}

}
