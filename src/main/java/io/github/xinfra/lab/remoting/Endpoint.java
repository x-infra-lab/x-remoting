package io.github.xinfra.lab.remoting;


import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@EqualsAndHashCode
@Getter
@Setter
public class Endpoint {

    private ProtocolType protocolType;

    private String ip;

    private int port;

    public Endpoint(ProtocolType protocolType, String ip, int port) {
        this.protocolType = protocolType;
        this.ip = ip;
        this.port = port;
    }
}
