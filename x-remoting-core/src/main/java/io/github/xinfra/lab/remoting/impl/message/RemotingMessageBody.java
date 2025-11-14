package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageBody;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class RemotingMessageBody implements MessageBody {


	private byte[] bodyData;


	private byte[] typeData;

	private String type;

	private byte[] valueData;


	private Object value;

	public RemotingMessageBody() {
	}

	public RemotingMessageBody(byte[] bodyData) {
		this.bodyData = bodyData;
	}

	@Override
	public void serialize(Serializer serializer) throws SerializeException {

	}

	@Override
	public void deserialize(Serializer serializer) throws DeserializeException {

	}

	@Override
	public byte[] data() {
		return bodyData;
	}

}
