package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;


public interface ConnectionManager {

    Connection getConnection(Endpoint endpoint) throws RemotingException;

}
