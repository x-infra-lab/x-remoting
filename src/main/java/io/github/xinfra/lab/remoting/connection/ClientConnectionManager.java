package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class ClientConnectionManager extends AbstractConnectionManager {

	@AccessForTest
	protected Reconnector reconnector;

	public ClientConnectionManager(Protocol protocol) {
		this.connectionFactory = new DefaultConnectionFactory(protocol, defaultChannelSuppliers());
	}

	public ClientConnectionManager(Protocol protocol, ConnectionConfig connectionConfig) {
		this.connectionFactory = new DefaultConnectionFactory(protocol, defaultChannelSuppliers(), connectionConfig);
	}

	public ClientConnectionManager(Protocol protocol, ConnectionManagerConfig connectionManagerConfig) {
		super(connectionManagerConfig);
		this.connectionFactory = new DefaultConnectionFactory(protocol, defaultChannelSuppliers());
	}

	public ClientConnectionManager(Protocol protocol, ConnectionConfig connectionConfig,
			ConnectionManagerConfig connectionManagerConfig) {
		super(connectionManagerConfig);
		this.connectionFactory = new DefaultConnectionFactory(protocol, defaultChannelSuppliers(), connectionConfig);
	}

	private List<Supplier<ChannelHandler>> defaultChannelSuppliers() {
		ProtocolHeartBeatHandler protocolHeartBeatHandler = new ProtocolHeartBeatHandler();
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
	public Connection connect(SocketAddress socketAddress) throws RemotingException {
		ConnectionHolder connectionHolder = connections.get(socketAddress);
		if (connectionHolder == null) {
			connectionHolder = createConnectionHolder(socketAddress);
		}
		createConnectionForHolder(socketAddress, connectionHolder, config.getConnectionNumPreEndpoint());

		return connectionHolder.get();
	}

	@Override
	public synchronized Connection get(SocketAddress socketAddress) throws RemotingException {
		ensureStarted();
		Validate.notNull(socketAddress, "socketAddress can not be null");

		ConnectionHolder connectionHolder = connections.get(socketAddress);
		if (connectionHolder == null) {
			return connect(socketAddress);
		}

		return connectionHolder.get();
	}

	@Override
	public Reconnector reconnector() {
		return reconnector;
	}

	@Override
	public void startup() {
		super.startup();

		reconnector = new DefaultReconnector(this);
		reconnector.startup();
	}

	@Override
	public synchronized void shutdown() {
		if (reconnector != null) {
			for (SocketAddress socketAddress : connections.keySet()) {
				reconnector.disableReconnect(socketAddress);
			}

			super.shutdown();

			reconnector.shutdown();
		}
	}

}
