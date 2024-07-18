package io.github.xinfra.lab.remoting.protocol;


import lombok.EqualsAndHashCode;
import lombok.ToString;


@EqualsAndHashCode
@ToString
public class ProtocolType {

    private String name;

    private byte[] protocolCode;

    public ProtocolType(String name, byte[] protocolCode) {
        this.name = name;
        this.protocolCode = protocolCode;
    }

    public byte[] protocolCode() {
        return this.protocolCode;
    }

    public String name() {
        return name;
    }
}
