package io.github.xinfra.lab.remoting.common;

import java.util.concurrent.atomic.AtomicBoolean;

public class AbstractLifeCycle implements LifeCycle {

    private final AtomicBoolean started = new AtomicBoolean(false);

    @Override
    public void startup() {
        if (started.compareAndSet(false, true)) {
            return;
        }
        throw new IllegalStateException(String.format(
                "Component(%s) has started", getClass()
                        .getSimpleName()));
    }

    @Override
    public void shutdown() {
        if (started.compareAndSet(true, false)) {
            return;
        }
        throw new IllegalStateException(String.format(
                "Component(%s) has shutdown", getClass()
                        .getSimpleName()));
    }

    @Override
    public boolean isStarted() {
        return started.get();
    }

    protected void ensureStarted() {
        if (!isStarted()) {
            throw new IllegalStateException(String.format(
                    "Component(%s) has not been started yet, please startup first!", getClass()
                            .getSimpleName()));
        }
    }
}
