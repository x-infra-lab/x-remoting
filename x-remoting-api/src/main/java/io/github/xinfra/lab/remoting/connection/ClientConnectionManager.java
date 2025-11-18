package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class ClientConnectionManager extends AbstractConnectionManager {

	@AccessForTest
	protected Reconnector reconnector = new DefaultReconnector(this);

	@AccessForTest
	protected Heartbeater heartbeater = new DefaultHeartbeater();

	public ClientConnectionManager(Protocol protocol) {
		this.connectionFactory = new DefaultConnectionFactory(protocol, defaultChannelSuppliers());
	}

	public ClientConnectionManager(Protocol protocol, ConnectionFactoryConfig connectionFactoryConfig) {
		this.connectionFactory = new DefaultConnectionFactory(protocol, defaultChannelSuppliers(),
				connectionFactoryConfig);
	}

	public ClientConnectionManager(Protocol protocol, ConnectionManagerConfig connectionManagerConfig) {
		super(connectionManagerConfig);
		this.connectionFactory = new DefaultConnectionFactory(protocol, defaultChannelSuppliers());
	}

	public ClientConnectionManager(Protocol protocol, ConnectionFactoryConfig connectionFactoryConfig,
			ConnectionManagerConfig connectionManagerConfig) {
		super(connectionManagerConfig);
		this.connectionFactory = new DefaultConnectionFactory(protocol, defaultChannelSuppliers(),
				connectionFactoryConfig);
	}

	private List<Supplier<ChannelHandler>> defaultChannelSuppliers() {
		ProtocolHeartBeatHandler protocolHeartBeatHandler = new ProtocolHeartBeatHandler(heartbeater);
		ProtocolHandler protocolHandler = new ProtocolHandler();
		ConnectionEventHandler connectionEventHandler = new ConnectionEventHandler(this);

		List<Supplier<ChannelHandler>> channelHandlerSuppliers = new ArrayList<>();
		// encoder and decoder not @ChannelHandler.Sharable marked. it need create
		// instance everytime
		channelHandlerSuppliers.add(ProtocolEncoder::new);
		channelHandlerSuppliers.add(ProtocolDecoder::new);
		channelHandlerSuppliers.add(() -> protocolHeartBeatHandler);
		channelHandlerSuppliers.add(() -> protocolHandler);
		channelHandlerSuppliers.add(() -> connectionEventHandler);
		return channelHandlerSuppliers;
	}

	@Override
	public synchronized Connection connect(SocketAddress socketAddress) throws RemotingException {
		ensureStarted();
		Connections connections = this.connections.get(socketAddress);
		if (connections == null) {
			connections = createConnectionHolder(socketAddress);
		}
		createConnectionForHolder(socketAddress, connections, config.getConnectionNumPreEndpoint());

		return connections.get();
	}

	@Override
	public Connection get(SocketAddress socketAddress) throws RemotingException {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress can not be null");

		Connections connections = this.connections.get(socketAddress);
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
	public Heartbeater heartbeatTrigger() {
		return null;
	}

	@Override
	public void startup() {
		super.startup();
		reconnector.startup();
	}

	@Override
	public synchronized void shutdown() {
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
