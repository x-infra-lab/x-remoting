package io.github.xinfra.lab.remoting.exception;

public class TimeoutException extends RemotingException {
    public TimeoutException() {
    }

    public TimeoutException(String message) {
        super(message);
    }
}
