package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageHeader;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

public class RpcMessageHeader implements Serializable, MessageHeader {

	@Getter
	@Setter
	private byte[] headerData;



	public RpcMessageHeader() {
	}

	public RpcMessageHeader(byte[] headerData) {
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
