package io.github.xinfra.lab.remoting.rpc.exception;

import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import lombok.Getter;

public class ResponseStatusRuntimeException extends RuntimeException {

	@Getter
	ResponseStatus responseStatus;

	public ResponseStatusRuntimeException(ResponseStatus responseStatus) {
		this.responseStatus = responseStatus;
	}

	public ResponseStatusRuntimeException(ResponseStatus responseStatus, Throwable cause) {
		super(cause);
		this.responseStatus = responseStatus;
	}

}
