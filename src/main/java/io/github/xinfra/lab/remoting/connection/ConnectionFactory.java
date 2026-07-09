package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.exception.RemotingException;

import java.io.Closeable;
import java.net.InetSocketAddress;

public interface ConnectionFactory extends Closeable {

	Connection create(InetSocketAddress socketAddress) throws RemotingException;

}
