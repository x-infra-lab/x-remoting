package io.github.xinfra.lab.remoting.impl.handler;

import io.github.xinfra.lab.remoting.impl.handler.RequestHandler;

public class ExceptionRequestHandler implements RequestHandler<String, String> {

	@Override
	public String handle(String request) {
		throw new IllegalArgumentException("handle exception");
	}

}
