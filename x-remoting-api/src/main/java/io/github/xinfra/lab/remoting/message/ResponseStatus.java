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
		switch (status) {
			case 0:
				return OK;
			case 1:
				return UnKnownError;
			case 2:
				return SendFailed;
			case 3:
				return Timeout;
			case 4:
				return ConnectionClosed;
			case 5:
				return Cancelled;
			case 6:
				return NotFound;
			case 7:
				return InternalError;
			case 8:
				return ResourceExhausted;
			case 9:
				return ServiceUnavailable;
			default:
				throw new IllegalArgumentException("Unknown status: " + status);
		}
	}

}
