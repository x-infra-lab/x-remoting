package io.github.xinfra.lab.remoting.message;


import java.net.SocketAddress;

public interface MessageFactory {


    Message createSendFailResponseMessage(SocketAddress remoteAddress, Throwable cause);

    Message createTimeoutResponseMessage(SocketAddress remoteAddress);

     Message createRequestMessage();
}
