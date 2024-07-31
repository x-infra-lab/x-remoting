package io.github.xinfra.lab.remoting.rpc.exception;

import io.github.xinfra.lab.remoting.exception.RemotingException;

public class RpcServerException extends RemotingException {

    public RpcServerException(String message) {
        super(message);
    }

    public RpcServerException(Throwable cause) {
        super(cause);
    }

}
