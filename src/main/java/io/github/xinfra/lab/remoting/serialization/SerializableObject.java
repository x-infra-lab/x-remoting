package io.github.xinfra.lab.remoting.serialization;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;

import java.util.List;

public interface SerializableObject {

	void serialize(Serializer serializer) throws SerializeException;

	void deserialize(Serializer serializer) throws DeserializeException;

	List<byte[]> getData();

	int getDataTotalLength();

}
