package io.github.xinfra.lab.remoting.serialization;

import java.util.HashMap;
import java.util.Map;

public class SerializationManager {

	private static Map<SerializationType, Serializer> serializerMap = new HashMap<>();

	static {
		serializerMap.put(SerializationType.HESSION, new HessionSerializer());
	}

	public static Serializer getSerializer(SerializationType type) {
		return serializerMap.get(type);
	}

}
