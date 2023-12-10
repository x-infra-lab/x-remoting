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

    private int connectTimeoutMills;

    private int connNum = 1;

    private boolean connWarmup;

}
