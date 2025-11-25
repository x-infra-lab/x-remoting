package io.github.xinfra.lab.remoting.server;

import io.github.xinfra.lab.remoting.annotation.AccessForTest;
import io.github.xinfra.lab.remoting.common.AbstractLifeCycle;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.common.Resource;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.connection.ConnectionEventHandler;
import io.github.xinfra.lab.remoting.connection.ConnectionEventProcessor;
import io.github.xinfra.lab.remoting.connection.DefaultConnectionEventProcessor;
import io.github.xinfra.lab.remoting.connection.ProtocolDecoder;
import io.github.xinfra.lab.remoting.connection.ProtocolEncoder;
import io.github.xinfra.lab.remoting.connection.ProtocolHandler;
import io.github.xinfra.lab.remoting.connection.ServerConnectionManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractServer extends AbstractLifeCycle implements Server {

	protected SocketAddress localAddress;

	private Executor executor;

	private Resource<ExecutorService> defaultExecutorResource = new Resource<ExecutorService>() {

		ExecutorService defaultExecutor;

		@Override
		public ExecutorService get() {
			if (defaultExecutor == null) {
				defaultExecutor = Executors
					.newCachedThreadPool(new NamedThreadFactory("RemotingClient-Server-Default-Executor"));
			}
			return defaultExecutor;
		}

		@Override
		public void close() {
			if (defaultExecutor != null) {
				defaultExecutor.shutdown();
			}
		}
	};

	private Resource<Timer> defaultTimerResource = new Resource<Timer>() {

		Timer defaultTimer;

		@Override
		public Timer get() {
			if (defaultTimer == null) {
				defaultTimer = new HashedWheelTimer(new NamedThreadFactory("RemotingClient-Server-Timer"));
			}
			return defaultTimer;
		}

		@Override
		public void close() {
			if (defaultTimer != null) {
				defaultTimer.stop();
			}
		}
	};

	private Timer timer;

	private ServerBootstrap serverBootstrap;

	private Channel serverChannel;

	private final EventLoopGroup bossGroup = Epoll.isAvailable()
			? new EpollEventLoopGroup(1, new NamedThreadFactory("RemotingServer-IO-Boss"))
			: new NioEventLoopGroup(1, new NamedThreadFactory("RemotingServer-IO-Boss"));

	private final EventLoopGroup workerGroup = Epoll.isAvailable()
			? new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
					new NamedThreadFactory("RemotingServer-IO-Worker"))
			: new NioEventLoopGroup(Runtime.getRuntime().availableProcessors() * 2,
					new NamedThreadFactory("RemotingServer-IO-Worker"));

	private static final Class<? extends ServerChannel> serverChannelClass = Epoll.isAvailable()
			? EpollServerSocketChannel.class : NioServerSocketChannel.class;

	private ChannelHandler connectionEventHandler;

	private ChannelHandler handler;

	private ChannelHandler serverIdleHandler = new ServerIdleHandler();

	protected ServerConnectionManager connectionManager;

	private ServerConfig config;

	private ConnectionEventProcessor connectionEventProcessor;

	public AbstractServer(ServerConfig config) {
		Validate.notNull(config, "RemotingServerConfig can not be null");
		Validate.inclusiveBetween(0, 0xFFFF, config.getPort(), "port out of range: " + config.getPort());

		this.config = config;
		if (config.getExecutor() != null) {
			this.executor = config.getExecutor();
		}
		else {
			this.executor = defaultExecutorResource.get();
		}
		if (config.getTimer() != null) {
			this.timer = config.getTimer();
		}
		else {
			this.timer = defaultTimerResource.get();
		}

		this.handler = new ProtocolHandler();
		if (this.config.isManageConnection()) {
			this.connectionManager = new ServerConnectionManager();
			this.connectionEventHandler = new ConnectionEventHandler(this.connectionManager);
		}
		else {
			this.connectionEventProcessor = new DefaultConnectionEventProcessor();
			this.connectionEventHandler = new ConnectionEventHandler(connectionEventProcessor);
		}
	}

	@Override
	public void startup() {
		super.startup();
		if (this.connectionManager != null) {
			this.connectionManager.startup();
		}
		if (this.connectionEventProcessor != null) {
			this.connectionEventProcessor.startup();
		}
		this.serverBootstrap = new ServerBootstrap();
		this.serverBootstrap.group(bossGroup, workerGroup)
			.channel(serverChannelClass)
			.childOption(ChannelOption.SO_KEEPALIVE, true)
			.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				protected void initChannel(SocketChannel channel) throws Exception {
					ChannelPipeline pipeline = channel.pipeline();

					pipeline.addLast("encoder", new ProtocolEncoder());
					pipeline.addLast("decoder", new ProtocolDecoder());

					if (config.isIdleSwitch()) {
						pipeline.addLast("idleStateHandler", new IdleStateHandler(config.getIdleReaderTimeout(),
								config.getIdleWriterTimeout(), config.getIdleAllTimeout(), TimeUnit.MILLISECONDS));
						pipeline.addLast("serverIdleHandler", serverIdleHandler);
					}
					pipeline.addLast("handler", handler);
					pipeline.addLast("connectionEventHandler", connectionEventHandler);

					createConnection(channel);
				}
			});

		try {
			this.localAddress = new InetSocketAddress(InetAddress.getLocalHost(), config.getPort());
			ChannelFuture channelFuture = this.serverBootstrap.bind(localAddress).sync();
			if (!channelFuture.isSuccess()) {
				throw channelFuture.cause();
			}
			// need update
			if (config.getPort() == 0) {
				this.localAddress = channelFuture.channel().localAddress();
			}
			serverChannel = channelFuture.channel();
		}
		catch (Throwable throwable) {
			throw new RuntimeException("serverBootstrap bind fail. ", throwable);
		}
	}

	@AccessForTest
	protected void createConnection(SocketChannel channel) {
		Connection connection = new Connection(protocol(), channel, executor, timer);
		if (config.isManageConnection()) {
			connectionManager.add(connection);
		}
	}

	@Override
	public void shutdown() {
		super.shutdown();
		if (serverChannel != null) {
			serverChannel.close();
		}
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		if (connectionManager != null) {
			connectionManager.shutdown();
		}
		if (this.connectionEventProcessor != null) {
			this.connectionEventProcessor.shutdown();
		}
		defaultExecutorResource.close();
		defaultTimerResource.close();
	}

	@Override
	public SocketAddress localAddress() {
		return this.localAddress;
	}

}
