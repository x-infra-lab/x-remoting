package io.github.xinfra.lab.remoting.message;

import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;

public interface MessagePayload {

	String payloadType();

	Object payload();

	void serialize() throws SerializeException;

	void deserialize() throws DeserializeException;

}
