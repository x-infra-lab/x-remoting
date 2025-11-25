package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageCodec;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;

public interface Protocol {

	ProtocolIdentifier protocolCode();

	MessageCodec messageCodec();

	MessageHandler messageHandler();

	MessageFactory messageFactory();

}
