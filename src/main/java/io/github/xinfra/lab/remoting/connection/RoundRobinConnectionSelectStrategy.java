package io.github.xinfra.lab.remoting.connection;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinConnectionSelectStrategy implements ConnectionSelectStrategy {

    private AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Connection select(List<Connection> connections) {
        if (connections == null || connections.isEmpty()) {
            return null;
        }

        if (connections.size() == 1) {
            return connections.get(0);
        }

        int i = Math.abs(counter.getAndIncrement());
        return connections.get(i % connections.size());
    }
}
