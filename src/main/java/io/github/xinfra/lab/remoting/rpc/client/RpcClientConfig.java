package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.connection.ConnectionConfig;
import io.github.xinfra.lab.remoting.connection.ConnectionManagerConfig;
import lombok.Data;

@Data
public class RpcClientConfig {

	private ConnectionConfig connectionConfig;

	private ConnectionManagerConfig connectionManagerConfig;

}
