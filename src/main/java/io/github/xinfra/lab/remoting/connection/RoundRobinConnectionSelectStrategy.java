package io.github.xinfra.lab.remoting.connection;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinConnectionSelectStrategy implements ConnectionSelectStrategy {

    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Connection select(List<Connection> connections) {
        int i = Math.abs(counter.getAndIncrement());

        return connections.get(i % connections.size());
    }
}
