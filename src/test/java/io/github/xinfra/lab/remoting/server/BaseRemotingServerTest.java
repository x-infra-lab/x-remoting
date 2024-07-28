package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import org.junit.jupiter.api.Test;

import static io.github.xinfra.lab.remoting.common.TestSocketUtils.findAvailableTcpPort;

public class BaseRemotingServerTest {

    private static ProtocolType test =new ProtocolType("BaseRemotingServerTest",
            new byte[]{0xb});
    @Test
    public void testBaseRemotingServer(){
        RemotingServerConfig config = new RemotingServerConfig();
        config.setPort(findAvailableTcpPort());

        BaseRemotingServer baseRemotingServer = new BaseRemotingServer(config) {
            @Override
            public ProtocolType protocolType() {
                return test;
            }
        };
    }
}
