package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.Decoder;
import io.github.xinfra.lab.remoting.codec.Encoder;
import io.github.xinfra.lab.remoting.message.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;


public interface Protocol {

    Encoder encoder();

    Decoder decoder();

    MessageHandler messageHandler();

    MessageFactory messageFactory();

    HeartbeatTrigger heartbeatTrigger();
}
