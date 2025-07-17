package io.github.xinfra.lab.remoting.serialization;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SerializationManager {

	private static Map<SerializationType, Serializer> serializerMap = new HashMap<>();

	private static Map<Byte, SerializationType> serializationTypeMap = new HashMap<>();

	static {
		registerSerializer(new HessionSerializer());
	}

	public static void registerSerializer(Serializer serializer) {
		SerializationType serializationType = serializer.serializationType();
		Serializer oldSerializer = serializerMap.put(serializationType, serializer);
		if (oldSerializer != serializer) {
			log.warn("replace serializationType:{} old:{} to new:{}", serializationType, oldSerializer, serializer);
		}
		SerializationType oldSerializationType = serializationTypeMap.put(serializationType.data(), serializationType);
		if (oldSerializationType != serializationType) {
			log.warn("replace serializationType data:{} old:{} to new:{}", serializationType.data(),
					oldSerializationType, serializationType);
		}
	}

	public static Serializer getSerializer(SerializationType type) {
		return serializerMap.get(type);
	}

	public static SerializationType valueOf(byte serializationTypeData) {
		return serializationTypeMap.get(serializationTypeData);
	}

}
