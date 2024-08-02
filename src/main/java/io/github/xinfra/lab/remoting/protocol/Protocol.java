package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.heartbeat.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;

import java.io.Closeable;


public interface Protocol extends Closeable {

    byte[] protocolCode();

    MessageEncoder encoder();

    MessageDecoder decoder();

    MessageHandler messageHandler();

    MessageFactory messageFactory();

    HeartbeatTrigger heartbeatTrigger();

}
