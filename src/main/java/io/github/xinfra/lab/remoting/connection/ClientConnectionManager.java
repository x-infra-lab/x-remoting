package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.Validate;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class ClientConnectionManager extends AbstractConnectionManager {

	protected Reconnector reconnector;

	public ClientConnectionManager(Protocol protocol) {
		this(protocol, defaultChannelSuppliers(), ConnectionFactoryConfig.defaults(),
				ConnectionManagerConfig.defaults(), ReconnectConfig.defaults());
	}

	public ClientConnectionManager(Protocol protocol, List<Supplier<ChannelHandler>> channelHandlerSuppliers,
			ConnectionFactoryConfig connectionFactoryConfig, ConnectionManagerConfig connectionManagerConfig,
			ReconnectConfig reconnectConfig) {
		super(connectionManagerConfig);
		ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler(this);
		List<Supplier<ChannelHandler>> allSuppliers = new ArrayList<>(channelHandlerSuppliers);
		allSuppliers.add(() -> connectionEventHandler);
		this.connectionFactory = new DefaultConnectionFactory(protocol, allSuppliers, connectionFactoryConfig);
		this.reconnector = new DefaultReconnector(this, reconnectConfig);
	}

	public static List<Supplier<ChannelHandler>> defaultChannelSuppliers() {
		ProtocolHandler protocolHandler = new ProtocolHandler();
		List<Supplier<ChannelHandler>> suppliers = new ArrayList<>();
		suppliers.add(ProtocolEncoder::new);
		suppliers.add(ProtocolDecoder::new);
		suppliers.add(() -> protocolHandler);
		return suppliers;
	}

	@Override
	public Connection connect(InetSocketAddress socketAddress) throws RemotingException {
		ensureStarted();
		Connections connections = createConnections(socketAddress);
		createConnection(socketAddress, connections, config.getConnectionNumPerEndpoint());

		return connections.get();
	}

	@Override
	public Connection get(InetSocketAddress socketAddress) throws RemotingException {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress can not be null");

		Connections connections = this.connectionsMap.get(socketAddress);
		if (connections == null) {
			return connect(socketAddress);
		}

		return connections.get();
	}

	@Override
	public Reconnector reconnector() {
		return reconnector;
	}

	@Override
	public void startup() {
		super.startup();
		reconnector.startup();
	}

	@Override
	public void shutdown() {
		super.shutdown();
		try {
			connectionFactory.close();
		}
		catch (IOException e) {
			log.warn("connectionFactory close ex", e);
		}
		reconnector.shutdown();
	}

}
