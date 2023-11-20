package io.github.xinfra.lab.remoting.common;

import java.util.concurrent.atomic.AtomicBoolean;

public class AbstractLifeCycle implements LifeCycle {

    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public void startup() {
        if (started.compareAndSet(false, true)) {
            return;
        }
        throw new IllegalStateException("this component has started");
    }

    @Override
    public void shutdown() {
        if (started.compareAndSet(true, false)) {
            return;
        }
        throw new IllegalStateException("this component has closed");
    }

    @Override
    public boolean isStarted() {
        return started.get();
    }
}
