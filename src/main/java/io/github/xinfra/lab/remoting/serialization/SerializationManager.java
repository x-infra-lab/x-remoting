package io.github.xinfra.lab.remoting.serialization;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class SerializationManager {

	private static ConcurrentMap<Byte, Serializer> serializerMap = new ConcurrentHashMap<>();

	static {
		registerSerializer(new HessianSerializer());
		registerSerializer(new FurySerializer());
	}

	public static void registerSerializer(Serializer serializer) {
		SerializationType serializationType = serializer.getSerializationType();
		Serializer prev = serializerMap.put(serializationType.getCode(), serializer);
		if (prev != null && prev != serializer) {
			log.warn("replaced serializer for type code {}: {} -> {}", serializationType.getCode(), prev, serializer);
		}
	}

	public static Serializer getSerializer(SerializationType type) {
		return serializerMap.get(type.getCode());
	}

	public static SerializationType valueOf(byte code) {
		return SerializationType.valueOf(code);
	}

}
