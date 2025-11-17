package io.github.xinfra.lab.remoting.message;

public class Headers {

	public static final MessageHeaders.Key<String> heartbeatKey = MessageHeaders.Key.stringKey("x-heartbeat");

	public static final String heartbeatValue = "1";

	public static final MessageHeaders.Key<String> onewayRequestKey = MessageHeaders.Key.stringKey("x-oneway");

	public static final String onewayRequestValue = "1";

}
