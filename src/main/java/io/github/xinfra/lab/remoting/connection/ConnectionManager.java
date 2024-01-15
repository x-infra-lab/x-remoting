package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;


public interface ConnectionManager {

    Connection getOrCreateIfAbsent(Endpoint endpoint) throws RemotingException;

    void check(Connection connection) throws RemotingException;

    void remove(Connection connection) throws RemotingException;
}
