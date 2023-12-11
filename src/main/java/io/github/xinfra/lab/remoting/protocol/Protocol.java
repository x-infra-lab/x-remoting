package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageDecoder;
import io.github.xinfra.lab.remoting.codec.MessageEncoder;
import io.github.xinfra.lab.remoting.message.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;


public interface Protocol {

    MessageEncoder encoder();

    MessageDecoder decoder();

    MessageHandler messageHandler();

    MessageFactory messageFactory();

    HeartbeatTrigger heartbeatTrigger();

}
