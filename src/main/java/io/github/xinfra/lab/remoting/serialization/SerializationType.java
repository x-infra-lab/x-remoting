package io.github.xinfra.lab.remoting.serialization;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public interface SerializationType {

	ConcurrentMap<Byte, SerializationType> REGISTRY = new ConcurrentHashMap<>();

	byte getCode();

	SerializationType Hessian = create((byte) 1);

	SerializationType Fury = create((byte) 2);

	static SerializationType create(byte code) {
		SerializationType type = () -> code;
		register(type);
		return type;
	}

	static void register(SerializationType type) {
		SerializationType prev = REGISTRY.putIfAbsent(type.getCode(), type);
		if (prev != null && prev != type) {
			throw new IllegalArgumentException("SerializationType code " + type.getCode() + " is already registered");
		}
	}

	static SerializationType valueOf(byte code) {
		SerializationType type = REGISTRY.get(code);
		if (type == null) {
			throw new IllegalArgumentException("Unknown serialization type: " + code);
		}
		return type;
	}

}
