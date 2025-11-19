package io.github.xinfra.lab.remoting.message;

import static io.github.xinfra.lab.remoting.message.Headers.onewayRequestKey;
import static io.github.xinfra.lab.remoting.message.Headers.onewayRequestValue;

public class Requests {

	public static void markOnewayRequest(RequestMessage requestMessage) {
		MessageHeaders header = requestMessage.headers();
		header.put(onewayRequestKey, onewayRequestValue);
	}

	public static boolean isOnewayRequest(RequestMessage requestMessage) {
		MessageHeaders header = requestMessage.headers();
		return header.contains(onewayRequestKey);
	}

}
