package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * per message header
 * |key-length:short|value-type-length:short|value-length:short|key:bytes|value-type:bytes|value:bytes|
 */
@Slf4j
public class DefaultMessageHeaders implements MessageHeaders {

	private ConcurrentHashMap<Pair<String, String>, Supplier<?>> headers = new ConcurrentHashMap<>();

	byte[] headerData;

	boolean serialized;

	boolean deserialized;

	private static final int KEY_LENGTH_SIZE = Short.BYTES;

	private static final int VALUE_TYPE_LENGTH_SIZE = Short.BYTES;

	private static final int VALUE_LENGTH_SIZE = Short.BYTES;

	private static final int HEADER_SIZE = KEY_LENGTH_SIZE + VALUE_TYPE_LENGTH_SIZE + VALUE_LENGTH_SIZE;

	public DefaultMessageHeaders() {
	}

	public DefaultMessageHeaders(byte[] headerData) {
		this.headerData = headerData;
	}

	@Override
	public <T> void put(Key<T> key, T value) {
		headers.put(Pair.of(key.getName(), key.getType().getName()), () -> value);
	}

	@Override
	public <T> T get(Key<T> key) {
		Supplier<?> supplier = headers.get(Pair.of(key.getName(), key.getType().getName()));
		if (supplier != null) {
			return (T) supplier.get();
		}
		return null;
	}

	@Override
	public boolean contains(Key<?> key) {
		return headers.contains(Pair.of(key.getName(), key.getType().getName()));
	}

	@Override
	public void serialize(Serializer serializer) throws SerializeException {
		if (!serialized) {
			serialized = true;
			if (headers.isEmpty()) {
				headerData = new byte[0];
				return;
			}
			ByteBuf buf = null;
			try {
				buf = ByteBufAllocator.DEFAULT.heapBuffer();
				for (Map.Entry<Pair<String, String>, Supplier<?>> entry : headers.entrySet()) {
					Pair<String, String> pair = entry.getKey();
					Object value = entry.getValue().get();

					byte[] keyData = pair.getLeft().getBytes(StandardCharsets.UTF_8);
					byte[] valueTypeData = pair.getRight().getBytes(StandardCharsets.UTF_8);
					byte[] valueData = serializer.serialize(value);
					buf.writeShort(keyData.length);
					buf.writeShort(valueTypeData.length);
					buf.writeShort(valueData.length);
					buf.writeBytes(keyData);
					buf.writeBytes(valueTypeData);
					buf.writeBytes(valueData);
				}

				headerData = new byte[buf.readableBytes()];
				buf.readBytes(headerData);
			}
			finally {
				if (buf != null) {
					buf.release();
				}
			}
		}
	}

	@Override
	public void deserialize(Serializer serializer) throws DeserializeException {
		if (!deserialized) {
			deserialized = true;
			if (headerData == null) {
				return;
			}
			ByteBuf byteBuf = null;
			try {
				byteBuf = Unpooled.wrappedBuffer(headerData);

				while (byteBuf.readableBytes() >= HEADER_SIZE) {
					short keyLength = byteBuf.readShort();
					short valueTypeLength = byteBuf.readShort();
					short valueLength = byteBuf.readShort();
					int dataLength = keyLength + valueTypeLength + valueLength;
					if (byteBuf.readableBytes() < dataLength) {
						log.error("Invalid header data:{}", headerData);
						throw new DeserializeException("Invalid header data");
					}

					String key = byteBuf.readCharSequence(keyLength, StandardCharsets.UTF_8).toString();
					String valueType = byteBuf.readCharSequence(valueTypeLength, StandardCharsets.UTF_8).toString();
					byte[] valueData = new byte[valueLength];
					byteBuf.readBytes(valueData);

					// lazy deserialization
					headers.put(Pair.of(key, valueType), new Supplier<Object>() {
						private Object value;

						@Override
						public Object get() {
							try {
								if (value != null) {
									return value;
								}
								return serializer.deserialize(valueData, (Class<?>) Class.forName(valueType));
							}
							catch (Exception e) {
								log.error("Deserialize header value failed", e);
								throw new RuntimeException("Deserialize header value failed", e);
							}
						}
					});
				}
			}
			finally {
				if (byteBuf != null) {
					byteBuf.release();
				}
			}
		}
	}

	@Override
	public byte[] data() {
		return headerData;
	}

}
