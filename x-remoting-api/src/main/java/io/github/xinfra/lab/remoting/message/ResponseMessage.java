package io.github.xinfra.lab.remoting.message;

public interface ResponseMessage extends Message {

	int status();

	boolean isOk();

}
