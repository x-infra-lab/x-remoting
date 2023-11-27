package io.github.xinfra.lab.remoting;


import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@EqualsAndHashCode
@Getter
@Setter
public class Endpoint {

    private String ip;

    private int port;

    private int connectTimeoutMills;

    private int connNum = 1;

    private boolean connWarmup;

}
