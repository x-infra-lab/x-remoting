package io.github.xinfra.lab.remoting.exception;

public class SerializeException extends RemotingException {
    public SerializeException(String message) {
        super(message);
    }

    public SerializeException(String message, Throwable cause) {
        super(message, cause);
    }
}
