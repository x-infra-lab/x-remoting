package io.github.xinfra.lab.remoting.serialization;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SerializationManager {

	private static Map<SerializationType, Serializer> serializerMap = new HashMap<>();

	static {
		registerSerializer(new HessionSerializer());
	}

	public static void registerSerializer(Serializer serializer) {
		Serializer oldSerializer = serializerMap.put(serializer.serializationType(), serializer);
		if (oldSerializer != serializer) {
			log.warn("replace serializationType:{} old:{} to new:{}", serializer.serializationType(), oldSerializer,
					serializer);
		}
	}

	public static Serializer getSerializer(SerializationType type) {
		return serializerMap.get(type);
	}

}
