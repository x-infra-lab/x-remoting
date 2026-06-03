package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
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

	@AccessForTest
	protected Reconnector reconnector;

	@AccessForTest
	protected Heartbeater heartbeater = new DefaultHeartbeater();

	public ClientConnectionManager(Protocol protocol) {
		this(protocol, ConnectionFactoryConfig.defaults(), ConnectionManagerConfig.defaults(),
				ReconnectConfig.defaults());
	}

	public ClientConnectionManager(Protocol protocol, ConnectionFactoryConfig connectionFactoryConfig) {
		this(protocol, connectionFactoryConfig, ConnectionManagerConfig.defaults(), ReconnectConfig.defaults());
	}

	public ClientConnectionManager(Protocol protocol, ConnectionManagerConfig connectionManagerConfig) {
		this(protocol, ConnectionFactoryConfig.defaults(), connectionManagerConfig, ReconnectConfig.defaults());
	}

	public ClientConnectionManager(Protocol protocol, ConnectionFactoryConfig connectionFactoryConfig,
			ConnectionManagerConfig connectionManagerConfig) {
		this(protocol, connectionFactoryConfig, connectionManagerConfig, ReconnectConfig.defaults());
	}

	public ClientConnectionManager(Protocol protocol, ConnectionFactoryConfig connectionFactoryConfig,
			ConnectionManagerConfig connectionManagerConfig, ReconnectConfig reconnectConfig) {
		super(connectionManagerConfig);
		this.connectionFactory = new DefaultConnectionFactory(protocol, defaultChannelSuppliers(),
				connectionFactoryConfig);
		this.reconnector = new DefaultReconnector(this, reconnectConfig);
	}

	private List<Supplier<ChannelHandler>> defaultChannelSuppliers() {
		ProtocolHeartBeatHandler protocolHeartBeatHandler = new ProtocolHeartBeatHandler(heartbeater);
		ProtocolHandler protocolHandler = new ProtocolHandler();
		ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler(this);

		List<Supplier<ChannelHandler>> channelHandlerSuppliers = new ArrayList<>();
		// getEncoder and getDecoder not @ChannelHandler.Sharable marked. it need create
		// instance everytime
		channelHandlerSuppliers.add(ProtocolEncoder::new);
		channelHandlerSuppliers.add(ProtocolDecoder::new);
		channelHandlerSuppliers.add(() -> protocolHeartBeatHandler);
		channelHandlerSuppliers.add(() -> protocolHandler);
		channelHandlerSuppliers.add(() -> connectionEventHandler);
		return channelHandlerSuppliers;
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
	public Heartbeater heartbeater() {
		return heartbeater;
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
