package io.github.xinfra.lab.remoting.rpc.message;

import static io.github.xinfra.lab.remoting.rpc.message.HeaderConstants.onewayRequestKey;
import static io.github.xinfra.lab.remoting.rpc.message.HeaderConstants.onewayRequestValue;

public class Requests {

	public static void markOnewayRequest(RequestMessage requestMessage) {
		MessageHeaders header = requestMessage.getHeaders();
		header.put(onewayRequestKey, onewayRequestValue);
	}

	public static boolean isOnewayRequest(RequestMessage requestMessage) {
		MessageHeaders header = requestMessage.getHeaders();
		if (header == null) {
			return false;
		}
		return header.contains(onewayRequestKey);
	}

}
