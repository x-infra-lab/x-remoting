package io.github.xinfra.lab.remoting.message;

public interface MessagePayload {

    String payloadType();

    Object payload();
}
