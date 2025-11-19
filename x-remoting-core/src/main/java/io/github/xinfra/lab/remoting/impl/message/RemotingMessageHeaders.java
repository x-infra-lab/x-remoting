package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.message.DefaultMessageHeaders;


public class RemotingMessageHeaders extends DefaultMessageHeaders {

    public RemotingMessageHeaders() {
    }

    public RemotingMessageHeaders(byte[] headerData) {
        super(headerData);
    }

}
