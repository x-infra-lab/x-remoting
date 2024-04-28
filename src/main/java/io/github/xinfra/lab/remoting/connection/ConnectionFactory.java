package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.exception.RemotingException;


public interface ConnectionFactory  {
    Connection create(Endpoint endpoint, ConnectionConfig config) throws RemotingException;

}
