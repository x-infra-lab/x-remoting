package io.github.xinfra.lab.remoting.common;

import io.github.xinfra.lab.remoting.message.MessageHeader;
import io.github.xinfra.lab.remoting.message.RequestMessage;

import static io.github.xinfra.lab.remoting.message.MessageHeaders.heartbeatKey;
import static io.github.xinfra.lab.remoting.message.MessageHeaders.heartbeatValue;
import static io.github.xinfra.lab.remoting.message.MessageHeaders.onewayRequestKey;
import static io.github.xinfra.lab.remoting.message.MessageHeaders.onewayRequestValue;

public class Requests {

	public static void markHeartbeatRequest(RequestMessage requestMessage) {
		MessageHeader header = requestMessage.header();
		header.put(heartbeatKey, heartbeatValue);
	}

	public static void markOnewayRequest(RequestMessage requestMessage) {
		MessageHeader header = requestMessage.header();
		header.put(onewayRequestKey, onewayRequestValue);
	}

	public static boolean isHeartbeatRequest(RequestMessage requestMessage) {
		MessageHeader header = requestMessage.header();
		return header.contains(heartbeatKey);
	}

	public static boolean isOnewayRequest(RequestMessage requestMessage) {
		MessageHeader header = requestMessage.header();
		return header.contains(onewayRequestKey);
	}

}
