package io.github.xinfra.lab.remoting.impl.exception;

import io.github.xinfra.lab.remoting.exception.RemotingException;

public class RpcServerException extends RemotingException {

	public RpcServerException(String message) {
		super(message);
	}

	public RpcServerException(Throwable cause) {
		super(cause);
	}

	public RpcServerException(String message, Throwable cause) {
		super(message, cause);
	}

}
