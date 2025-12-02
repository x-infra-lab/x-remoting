package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageBody;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * |version:byte|type-length:short|type:bytes|value:bytes|
 */
public class RemotingMessageBody implements MessageBody {

	private byte[] bodyData;

	private int bodyDataTotalLength;

	private List<byte[]> bodyDataList;

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
				return;
			}

			bodyDataList = new ArrayList<>(1);

			String typeName = bodyValue.getClass().getName();
			byte[] typeData = typeName.getBytes(StandardCharsets.UTF_8);
			byte[] valueData = serializer.serialize(bodyValue);

			int dataLength = VERSION_SIZE + TYPE_LENGTH_SIZE + typeData.length + valueData.length;
			byte[] data = new byte[dataLength];
			ByteBuf buf = Unpooled.wrappedBuffer(data);
			buf.writerIndex(0);

			buf.writeByte(version);
			buf.writeShort(typeData.length);
			buf.writeBytes(typeData);
			buf.writeBytes(valueData);

			bodyDataList.add(data);
			bodyDataTotalLength += dataLength;
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
	public List<byte[]> getData() {
		return bodyDataList;
	}

	@Override
	public int getDataTotalLength() {
		return bodyDataTotalLength;
	}

}
