package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;

import java.util.Map;
import java.util.concurrent.Future;

public class ServerConnectionManager extends AbstractConnectionManager {

    @Override
    public Connection getOrCreateIfAbsent(Endpoint endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reconnect(Endpoint endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disableReconnect(Endpoint endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void enableReconnect(Endpoint endpoint) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Future<Void> asyncReconnect(Endpoint endpoint) {
        throw new UnsupportedOperationException();
    }

    public synchronized void shutdown() {
        super.shutdown();

        for (Map.Entry<Endpoint, ConnectionHolder> entry : connections.entrySet()) {
            Endpoint endpoint = entry.getKey();
            ConnectionHolder connectionHolder = entry.getValue();
            connectionHolder.removeAndCloseAll();
            connections.remove(endpoint);
        }

    }
}
