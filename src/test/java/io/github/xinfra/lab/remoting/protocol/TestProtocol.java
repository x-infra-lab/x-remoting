package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.message.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;

public class TestProtocol implements Protocol {
    @Override
    public MessageEncoder encoder() {
        return null;
    }

    @Override
    public MessageDecoder decoder() {
        return null;
    }

    @Override
    public MessageHandler messageHandler() {
        return null;
    }

    @Override
    public MessageFactory messageFactory() {
        return null;
    }

    @Override
    public HeartbeatTrigger heartbeatTrigger() {
        return null;
    }
}
