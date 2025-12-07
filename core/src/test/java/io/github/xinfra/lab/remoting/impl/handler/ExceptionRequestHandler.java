package io.github.xinfra.lab.remoting.impl.handler;

public class ExceptionRequestHandler implements RequestHandler<String, String> {

	@Override
	public String handle(String request) {
		throw new IllegalArgumentException("handle exception");
	}

}
