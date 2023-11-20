package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;


public interface ConnectionManager {

    Connection getConnection(Endpoint endpoint);

}
