package io.github.xinfra.lab.remoting.rpc.handler;

public class ExceptionRequestHandler extends BlockingRequestHandler<String, String> {

	@Override
	public String handleRequest(String request) {
		throw new IllegalArgumentException("handle exception");
	}

}
