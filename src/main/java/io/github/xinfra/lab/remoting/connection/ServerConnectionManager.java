package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;

public class ServerConnectionManager extends AbstractConnectionManager {

    @Override
    public Connection getOrCreateIfAbsent(Endpoint endpoint) throws RemotingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reconnection(Endpoint endpoint) throws RemotingException {
        throw new UnsupportedOperationException();
    }
}
