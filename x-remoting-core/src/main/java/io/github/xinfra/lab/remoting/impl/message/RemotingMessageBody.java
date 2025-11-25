package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageBody;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;

/**
 * |version:byte|type-length:short|type:bytes|value:bytes|
 */
public class RemotingMessageBody implements MessageBody {

	private byte[] bodyData;

	/**
	 * for upgrades
	 */
	private byte version = 0x1;

	@Setter
	@Getter
	private Object bodyValue;

	private boolean serialized;

	private boolean deserialized;

	private static final int VERSION_SIZE = Byte.BYTES;

	private static final int TYPE_LENGTH_SIZE = Short.BYTES;

	public RemotingMessageBody() {
	}

	public RemotingMessageBody(Object bodyValue) {
		this.bodyValue = bodyValue;
	}

	public RemotingMessageBody(byte[] bodyData) {
		this.bodyData = bodyData;
	}

	@Override
	public void serialize(Serializer serializer) throws SerializeException {
		if (!serialized) {
			serialized = true;
			if (bodyValue == null) {
				bodyData = new byte[0];
				return;
			}

			ByteBuf buf = null;
			try {
				buf = ByteBufAllocator.DEFAULT.heapBuffer();

				buf.writeByte(version);

				String typeName = bodyValue.getClass().getName();
				byte[] typeData = typeName.getBytes(StandardCharsets.UTF_8);
				byte[] valueData = serializer.serialize(bodyValue);
				buf.writeShort(typeData.length);
				buf.writeBytes(typeData);
				buf.writeBytes(valueData);

				bodyData = new byte[buf.readableBytes()];
				buf.readBytes(bodyData);
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
			if (bodyData == null) {
				return;
			}
			try {
				ByteBuf byteBuf = Unpooled.wrappedBuffer(bodyData);
				version = byteBuf.readByte();
				short typeLength = byteBuf.readShort();
				String typeName = byteBuf.readCharSequence(typeLength, StandardCharsets.UTF_8).toString();
				int bodyDataLength = byteBuf.readableBytes();
				byte[] valueData = new byte[bodyDataLength];
				byteBuf.readBytes(valueData);
				bodyValue = serializer.deserialize(valueData, (Class<?>) Class.forName(typeName));
			}
			catch (ClassNotFoundException e) {
				throw new DeserializeException("Deserialize body value failed", e);
			}
		}
	}

	@Override
	public byte[] getData() {
		return bodyData;
	}

}
