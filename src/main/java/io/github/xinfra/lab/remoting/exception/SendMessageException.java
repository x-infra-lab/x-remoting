package io.github.xinfra.lab.remoting.exception;

public class SendMessageException extends RemotingException {

    public SendMessageException(String message) {
        super(message);
    }

    public SendMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public SendMessageException(Throwable cause) {
        super(cause);
    }
}
