package io.github.xinfra.lab.remoting.exception;

public class DeserializeException extends RemotingException {
    public DeserializeException(String message) {
        super(message);
    }

    public DeserializeException(String message, Throwable cause) {
        super(message, cause);
    }
}
