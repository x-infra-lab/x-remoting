package io.github.xinfra.lab.remoting.message;

public interface MessageType {

    byte data();

    MessageType heartbeat = () -> (byte) -1;

    MessageType request = () -> (byte) 0;

    MessageType response = () -> (byte) 1;



    static MessageType valueOf(byte data) {
        // todo
        return  null;
    }

}
