package io.github.xinfra.lab.remoting.serialization;

public enum SerializationType {

	HESSION;

	public byte data() {
		return (byte) this.ordinal();
	}

	public static SerializationType valueOf(byte data) {
		return SerializationType.values()[data];
	}

}
