package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.net.SocketAddress;


public interface ConnectionFactory {

    Connection create(SocketAddress socketAddress) throws RemotingException;

}
