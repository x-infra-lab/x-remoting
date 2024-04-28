package io.github.xinfra.lab.remoting.common;

public interface LifeCycle {
    void startup();

    void shutdown();

    boolean isStarted();
}
