package io.github.xinfra.lab.remoting.message;

public interface RpcStatusCode {
    int SUCCESS = 0;
    int UNKNOWN = 1;
    int ERROR = 2;

    int CLIENT_SEND_ERROR = 3;

    int TIMEOUT = 4;

    int CONNECTION_CLOSED = 5;

    int SERVER_EXCEPTION = 6;


}
