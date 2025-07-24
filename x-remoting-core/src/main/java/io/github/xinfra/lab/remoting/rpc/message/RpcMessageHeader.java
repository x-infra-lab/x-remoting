package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.message.MessageHeader;
import io.github.xinfra.lab.remoting.serialization.Serializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode
public class RpcMessageHeader implements Serializable, MessageHeader {


	@Override
	public byte[] serialize(Serializer serializer) throws SerializeException {
		// todo
		return null;
	}

	@Override
	public void deserialize(Serializer serializer, byte[] headerData) throws DeserializeException {
		// todo
	}
}
