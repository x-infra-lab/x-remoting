package io.github.xinfra.lab.remoting.protocol;

import io.github.xinfra.lab.remoting.codec.MessageCodec;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.MessageHandler;

import java.io.Closeable;

public interface Protocol extends Closeable {

	byte[] protocolCode();

	MessageCodec messageCodec();

	MessageHandler messageHandler();

	MessageFactory messageFactory();

}
