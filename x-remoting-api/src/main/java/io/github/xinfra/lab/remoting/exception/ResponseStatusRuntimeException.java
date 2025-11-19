package io.github.xinfra.lab.remoting.exception;

import io.github.xinfra.lab.remoting.message.ResponseStatus;
import lombok.Getter;

public class ResponseStatusRuntimeException extends RuntimeException {

    @Getter
    ResponseStatus responseStatus;

    public ResponseStatusRuntimeException(ResponseStatus responseStatus) {
        this.responseStatus = responseStatus;
    }


}
