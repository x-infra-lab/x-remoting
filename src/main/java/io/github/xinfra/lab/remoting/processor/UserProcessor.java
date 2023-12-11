package io.github.xinfra.lab.remoting.processor;

public interface UserProcessor<T> {
    String interest();

    Object handRequest(T request);
}
