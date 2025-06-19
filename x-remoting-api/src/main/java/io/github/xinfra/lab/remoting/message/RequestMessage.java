package io.github.xinfra.lab.remoting.message;


public interface RequestMessage extends Message {

    String path();

    default boolean isHeartbeat() {
        return false;
    }

}
