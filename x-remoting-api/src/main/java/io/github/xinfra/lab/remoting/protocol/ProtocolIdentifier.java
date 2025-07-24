package io.github.xinfra.lab.remoting.protocol;

public interface ProtocolIdentifier {

	byte[] code();

	default byte version(){
		return  0x01;
	}
}
