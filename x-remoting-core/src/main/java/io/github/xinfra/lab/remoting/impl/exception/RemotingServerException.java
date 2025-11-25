package io.github.xinfra.lab.remoting.impl.exception;

import io.github.xinfra.lab.remoting.exception.RemotingException;

public class RemotingServerException extends RemotingException {

	public RemotingServerException(String message) {
		super(message);
	}

	public RemotingServerException(Throwable cause) {
		super(cause);
	}

	public RemotingServerException(String message, Throwable cause) {
		super(message, cause);
	}

}
