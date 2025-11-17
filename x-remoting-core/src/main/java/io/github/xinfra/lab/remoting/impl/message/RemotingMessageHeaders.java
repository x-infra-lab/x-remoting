package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageHeaders;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public class RemotingMessageHeaders implements Serializable, MessageHeaders {

	@Getter
	@Setter
	private byte[] headerData;

	public RemotingMessageHeaders() {
	}

	public RemotingMessageHeaders(byte[] headerData) {
		this.headerData = headerData;
	}

	@Override
	public void serialize(Serializer serializer) throws SerializeException {

	}

	@Override
	public void deserialize(Serializer serializer) throws DeserializeException {

	}

	@Override
	public byte[] data() {
		return headerData;
	}

}
