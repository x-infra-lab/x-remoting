package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.heartbeat.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageFactory;
import lombok.Setter;

public class TestProtocol implements Protocol {


    @Setter
    private MessageEncoder testMessageEncoder;
    @Setter
    private MessageDecoder testMessageDecoder;
    @Setter
    private MessageHandler testMessageHandler;
    @Setter
    private RpcMessageFactory testMessageFactory;
    @Setter
    private HeartbeatTrigger testHeartbeatTrigger;

    @Override
    public MessageEncoder encoder() {
        return testMessageEncoder;
    }

    @Override
    public MessageDecoder decoder() {
        return testMessageDecoder;
    }

    @Override
    public MessageHandler messageHandler() {
        return testMessageHandler;
    }

    @Override
    public MessageFactory messageFactory() {
        return testMessageFactory;
    }

    @Override
    public HeartbeatTrigger heartbeatTrigger() {
        return testHeartbeatTrigger;
    }
}
