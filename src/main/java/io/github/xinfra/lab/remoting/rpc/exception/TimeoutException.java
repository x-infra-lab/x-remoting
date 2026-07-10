package io.github.xinfra.lab.remoting.rpc.exception;

import io.github.xinfra.lab.remoting.exception.RemotingException;

public class TimeoutException extends RemotingException {

	public TimeoutException() {
	}

	public TimeoutException(String message) {
		super(message);
	}

}
