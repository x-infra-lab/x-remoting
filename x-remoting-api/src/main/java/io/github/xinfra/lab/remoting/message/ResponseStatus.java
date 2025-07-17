package io.github.xinfra.lab.remoting.message;

public interface ResponseStatus {

	short status();

	ResponseStatus OK = () -> (short) 0;

	ResponseStatus Error = () -> (short) 1;

	ResponseStatus SendFailed = () -> (short) 2;

	ResponseStatus Timeout = () -> (short) 3;

	ResponseStatus ConnectionClosed = () -> (short) 4;

}
