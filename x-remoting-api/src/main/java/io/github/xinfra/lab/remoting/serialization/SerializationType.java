package io.github.xinfra.lab.remoting.serialization;

public interface SerializationType {

	byte data();

	SerializationType Hession = () -> (byte) -1;

}
