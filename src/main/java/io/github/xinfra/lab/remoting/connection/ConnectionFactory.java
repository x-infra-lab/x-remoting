package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.Endpoint;


public interface ConnectionFactory {
    Connection create(Endpoint endpoint) throws Exception;
}
