package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.connection.ConnectionConfig;
import io.github.xinfra.lab.remoting.connection.ConnectionManagerConfig;
import lombok.Data;

@Data
public class RemotingClientConfig {

	private ConnectionConfig connectionConfig;

	private ConnectionManagerConfig connectionManagerConfig;

}
