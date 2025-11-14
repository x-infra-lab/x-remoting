package io.github.xinfra.lab.remoting.message;

/**
 *
 */
public interface ResponseStatus {

	short status();

	ResponseStatus OK = () -> (short) 0;

	ResponseStatus UnKnownError = () -> (short) 1;

	ResponseStatus SendFailed = () -> (short) 2;

	ResponseStatus Timeout = () -> (short) 3;

	ResponseStatus ConnectionClosed = () -> (short) 4;

	ResponseStatus Cancelled = () -> (short) 5;

	ResponseStatus NotFound = () -> (short) 6;

	ResponseStatus InternalError = () -> (short) 7;

	ResponseStatus ResourceExhausted = () -> (short) 8;

	ResponseStatus ServiceUnavailable = () -> (short) 9;

	static ResponseStatus valueOf(short status) {
		// todo
		return null;
	}

}
