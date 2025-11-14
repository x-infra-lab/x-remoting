package io.github.xinfra.lab.remoting.message;

public class MessageHeaders {

	public static final MessageHeader.Key<String> heartbeatKey = MessageHeader.Key.stringKey("x-heartbeat");

	public static final String heartbeatValue = "1";

	public static final MessageHeader.Key<String> onewayRequestKey = MessageHeader.Key.stringKey("x-oneway");

	public static final String onewayRequestValue = "1";

}
