package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.io.Closeable;
import java.net.SocketAddress;

public interface ConnectionFactory extends Closeable {

	Connection create(SocketAddress socketAddress) throws RemotingException;

}
