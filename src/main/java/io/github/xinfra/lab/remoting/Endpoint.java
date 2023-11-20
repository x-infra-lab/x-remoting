package io.github.xinfra.lab.remoting;


import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

@EqualsAndHashCode
@Getter
@Setter
public class Endpoint {

    private String ip;

    private int port;

    private int connectTimeoutMills;

    private ProtocolType protocolType;

    private int connNum = 1;

    private boolean connWarmup;

    private Properties properties;
}
