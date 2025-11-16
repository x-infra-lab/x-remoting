package io.github.xinfra.lab.remoting.impl.server.handler;

import io.github.xinfra.lab.remoting.impl.handler.RequestHandler;

public class ExceptionRequestHandler implements RequestHandler<String, String> {

	@Override
	public String handle(String request) {
		throw new RuntimeException("handle exception");
	}

}
