package io.github.xinfra.lab.remoting.rpc.handler;

import lombok.Setter;

import java.util.concurrent.Executor;

public class EchoRequestHandler extends BlockingRequestHandler<EchoRequest, String> {

	@Setter
	private Executor executor;

	@Override
	public String handleRequest(EchoRequest request) {
		return "echo:" + request.getMsg();
	}

	@Override
	public Executor getExecutor() {
		return executor;
	}

}
