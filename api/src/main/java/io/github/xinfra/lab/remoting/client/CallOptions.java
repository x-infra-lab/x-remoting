package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.message.DefaultMessageHeaders;
import io.github.xinfra.lab.remoting.serialization.SerializationType;
import lombok.Data;

@Data
public class CallOptions {

	private int timeoutMills = 3000;

	private SerializationType serializationType = SerializationType.Hession;

	private DefaultMessageHeaders headers = new DefaultMessageHeaders();

}
