package io.github.xinfra.lab.remoting.message;

import static io.github.xinfra.lab.remoting.message.HeaderConstants.onewayRequestKey;
import static io.github.xinfra.lab.remoting.message.HeaderConstants.onewayRequestValue;

public class Requests {

	public static void markOnewayRequest(RequestMessage requestMessage) {
		MessageHeaders header = requestMessage.headers();
		header.put(onewayRequestKey, onewayRequestValue);
	}

	public static boolean isOnewayRequest(RequestMessage requestMessage) {
		MessageHeaders header = requestMessage.headers();
		if (header == null) {
			return false;
		}
		return header.contains(onewayRequestKey);
	}

}
