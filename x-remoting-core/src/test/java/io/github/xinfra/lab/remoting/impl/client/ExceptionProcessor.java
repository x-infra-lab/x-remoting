package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.impl.processor.UserProcessor;

public class ExceptionProcessor implements UserProcessor<ExceptionRequest> {

	@Override
	public String interest() {
		return ExceptionRequest.class.getName();
	}

	@Override
	public Object handRequest(ExceptionRequest request) {
		throw new RuntimeException(request.getErrorMsg());
	}

}
