package io.github.xinfra.lab.remoting.impl.client;

public class SimpleUserProcessor implements UserProcessor<SimpleRequest> {

	@Override
	public String interest() {
		return SimpleRequest.class.getName();
	}

	@Override
	public Object handRequest(SimpleRequest request) {
		return "echo:" + request.getMsg();
	}

}
