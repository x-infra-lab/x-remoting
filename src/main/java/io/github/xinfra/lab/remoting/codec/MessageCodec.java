package io.github.xinfra.lab.remoting.codec;

public interface MessageCodec {

	MessageEncoder getEncoder();

	MessageDecoder getDecoder();

}
