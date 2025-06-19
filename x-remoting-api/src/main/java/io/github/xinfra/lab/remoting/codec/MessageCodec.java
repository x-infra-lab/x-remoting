package io.github.xinfra.lab.remoting.codec;

public interface MessageCodec {

    MessageEncoder encoder();

    MessageDecoder decoder();


}
