package io.github.xinfra.lab.remoting.serialization;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import org.apache.fury.Fury;
import org.apache.fury.config.Language;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FurySerializer implements Serializer {

	private static final List<Class<?>> registeredClasses = new CopyOnWriteArrayList<>();

	private static final ThreadLocal<Fury> FURY_HOLDER = ThreadLocal.withInitial(() -> {
		Fury fury = Fury.builder().withLanguage(Language.JAVA).requireClassRegistration(true).build();
		for (Class<?> clazz : registeredClasses) {
			fury.register(clazz);
		}
		return fury;
	});

	public static void registerClass(Class<?> clazz) {
		registeredClasses.add(clazz);
	}

	@Override
	public SerializationType getSerializationType() {
		return SerializationType.Fury;
	}

	@Override
	public byte[] serialize(Object obj) throws SerializeException {
		try {
			return FURY_HOLDER.get().serialize(obj);
		}
		catch (Exception e) {
			throw new SerializeException("Fury serialize fail.", e);
		}
	}

	@Override
	public <T> T deserialize(byte[] data, Class<T> clazz) throws DeserializeException {
		try {
			Object obj = FURY_HOLDER.get().deserialize(data);
			return clazz.cast(obj);
		}
		catch (Exception e) {
			throw new DeserializeException("Fury deserialize fail.", e);
		}
	}

}
