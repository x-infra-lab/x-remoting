package io.github.xinfra.lab.remoting.protocol;


import lombok.EqualsAndHashCode;
import lombok.ToString;


@EqualsAndHashCode
@ToString
public class ProtocolType {

    private byte[] protocolCode;

    public ProtocolType(byte[] protocolCode) {
        this.protocolCode = protocolCode;
    }

    public byte[] protocolCode() {
        return this.protocolCode;
    }

}
