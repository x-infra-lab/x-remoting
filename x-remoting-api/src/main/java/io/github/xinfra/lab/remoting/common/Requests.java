package io.github.xinfra.lab.remoting.common;

import io.github.xinfra.lab.remoting.message.MessageHeaders;
import io.github.xinfra.lab.remoting.message.RequestMessage;

import static io.github.xinfra.lab.remoting.message.Headers.heartbeatKey;
import static io.github.xinfra.lab.remoting.message.Headers.heartbeatValue;
import static io.github.xinfra.lab.remoting.message.Headers.onewayRequestKey;
import static io.github.xinfra.lab.remoting.message.Headers.onewayRequestValue;

public class Requests {

	public static void markHeartbeatRequest(RequestMessage requestMessage) {
		MessageHeaders header = requestMessage.headers();
		header.put(heartbeatKey, heartbeatValue);
	}

	public static void markOnewayRequest(RequestMessage requestMessage) {
		MessageHeaders header = requestMessage.headers();
		header.put(onewayRequestKey, onewayRequestValue);
	}

	public static boolean isHeartbeatRequest(RequestMessage requestMessage) {
		MessageHeaders header = requestMessage.headers();
		return header.contains(heartbeatKey);
	}

	public static boolean isOnewayRequest(RequestMessage requestMessage) {
		MessageHeaders header = requestMessage.headers();
		return header.contains(onewayRequestKey);
	}

}
