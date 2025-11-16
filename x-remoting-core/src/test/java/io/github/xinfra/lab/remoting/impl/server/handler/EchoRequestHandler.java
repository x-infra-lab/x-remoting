package io.github.xinfra.lab.remoting.impl.server.handler;

import io.github.xinfra.lab.remoting.impl.handler.RequestHandler;

public class EchoRequestHandler implements RequestHandler<EchoRequest, String> {

	@Override
	public String handle(EchoRequest request) {
		return "echo:" + request.getMsg();
	}

}
