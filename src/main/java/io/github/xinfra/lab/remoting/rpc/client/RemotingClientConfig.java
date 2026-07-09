package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.connection.ConnectionFactoryConfig;
import io.github.xinfra.lab.remoting.connection.ConnectionManagerConfig;
import io.github.xinfra.lab.remoting.connection.ReconnectConfig;
import lombok.Data;

@Data
public class RemotingClientConfig {

	private ConnectionFactoryConfig connectionFactoryConfig;

	private ConnectionManagerConfig connectionManagerConfig;

	private ReconnectConfig reconnectConfig;

}
