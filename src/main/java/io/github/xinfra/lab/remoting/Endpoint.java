package io.github.xinfra.lab.remoting;


import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.Validate;


@EqualsAndHashCode
@Getter
public class Endpoint {

    private ProtocolType protocolType;

    private String ip;

    private int port;

    public Endpoint(ProtocolType protocolType, String ip, int port) {
        Validate.notNull(protocolType, "protocolType can not be null.");
        Validate.notBlank(ip, "ip can not be blank.");
        Validate.inclusiveBetween(0, 0xFFFF, port, "port out of range: " + port);
        this.protocolType = protocolType;
        this.ip = ip;
        this.port = port;
    }
}
