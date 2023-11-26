package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.channel.ChannelHandler;

public class DefaultConnectionFactory extends AbstractConnectionFactory {
    public DefaultConnectionFactory(ProtocolType protocolType, ChannelHandler handler) {
        super(protocolType,
                new ProtocolEncoder(),
                new ProtocolDecoder(),
                new HeartBeatHandler(),
                handler);
    }
}
