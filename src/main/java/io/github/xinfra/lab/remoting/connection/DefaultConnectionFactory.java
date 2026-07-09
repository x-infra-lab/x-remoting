package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.EpollUtils;
import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.common.Resource;
import io.github.xinfra.lab.remoting.common.Validate;
import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class DefaultConnectionFactory implements ConnectionFactory {

	private Protocol protocol;

	private Bootstrap bootstrap;

	private ConnectionFactoryConfig connectionFactoryConfig;

	private final EventLoopGroup workerGroup = EpollUtils.newEventLoopGroup(Runtime.getRuntime().availableProcessors(),
			new NamedThreadFactory("RemotingClient-Client-IO-Worker"));

	private Resource<ExecutorService> defaultExecutorResource = new Resource<ExecutorService>() {

		ExecutorService defaultExecutor;

		@Override
		public ExecutorService get() {
			if (defaultExecutor == null) {
				defaultExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(),
						new NamedThreadFactory("RemotingClient-Client-Default-Executor"));
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

	private ExecutorService executor;

	private Resource<Timer> defaultTimerResource = new Resource<Timer>() {

		Timer defaultTimer;

		@Override
		public Timer get() {
			if (defaultTimer == null) {
				defaultTimer = new HashedWheelTimer(new NamedThreadFactory("RemotingClient-Client-Timer"));
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

	private static final Class<? extends SocketChannel> channelClass = EpollUtils.clientChannelClass();

	public DefaultConnectionFactory(Protocol protocol, List<Supplier<ChannelHandler>> channelHandlerSuppliers) {
		this(protocol, channelHandlerSuppliers, ConnectionFactoryConfig.defaults());
	}

	// Q: why use Supplier to get ChannelHandler?
	// A: some ChannelHandler is not @ChannelHandler.Sharable. need create instance every
	// time
	public DefaultConnectionFactory(Protocol protocol, List<Supplier<ChannelHandler>> channelHandlerSuppliers,
			ConnectionFactoryConfig connectionFactoryConfig) {
		Validate.notNull(protocol, "getProtocol can not be null");
		Validate.notNull(channelHandlerSuppliers, "channelHandlers can not be null");
		Validate.notNull(connectionFactoryConfig, "connectionFactoryConfig can not be null");
		this.protocol = protocol;
		this.connectionFactoryConfig = connectionFactoryConfig;
		if (connectionFactoryConfig.getExecutor() != null) {
			this.executor = connectionFactoryConfig.getExecutor();
		}
		else {
			this.executor = defaultExecutorResource.get();
		}
		if (connectionFactoryConfig.getTimer() != null) {
			this.timer = connectionFactoryConfig.getTimer();
		}
		else {
			this.timer = defaultTimerResource.get();
		}

		bootstrap = new Bootstrap();
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionFactoryConfig.getConnectTimeout())
			.group(workerGroup)
			.channel(channelClass)
			.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					if (connectionFactoryConfig.isUseFlushConsolidation()) {
						pipeline.addLast("flushConsolidationHandler", new FlushConsolidationHandler());
					}
					if (connectionFactoryConfig.isIdleSwitch()) {
						pipeline.addLast("idleStateHandler",
								new IdleStateHandler(connectionFactoryConfig.getIdleReaderTimeout(),
										connectionFactoryConfig.getIdleWriterTimeout(),
										connectionFactoryConfig.getIdleAllTimeout(), TimeUnit.MILLISECONDS));
					}

					for (Supplier<ChannelHandler> supplier : channelHandlerSuppliers) {
						pipeline.addLast(supplier.get());
					}
				}
			});
	}

	@Override
	public Connection create(InetSocketAddress socketAddress) throws RemotingException {
		ChannelFuture future = bootstrap.connect(socketAddress);

		// Wait at most connectTimeout + 100ms so Netty's own CONNECT_TIMEOUT_MILLIS
		// always fires first and the future carries a real cause.
		long waitMillis = connectionFactoryConfig.getConnectTimeout() + 100L;
		try {
			if (!future.await(waitMillis, TimeUnit.MILLISECONDS)) {
				future.cancel(true);
				String errMsg = "Create connection to " + socketAddress + " timeout (" + waitMillis + "ms)!";
				log.warn(errMsg);
				throw new RemotingException(errMsg);
			}
		}
		catch (InterruptedException e) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			throw new RemotingException("Create connection to " + socketAddress + " interrupted", e);
		}
		if (future.isCancelled()) {
			String errMsg = "Create connection to " + socketAddress + " cancelled by user!";
			log.warn(errMsg);
			throw new RemotingException(errMsg);
		}
		if (!future.isSuccess()) {
			String errMsg = "Create connection to " + socketAddress + " error!";
			log.warn(errMsg);
			throw new RemotingException(errMsg, future.cause());
		}
		Channel channel = future.channel();
		return new Connection(protocol, channel, executor, timer);
	}

	@Override
	public void close() throws IOException {
		workerGroup.shutdownGracefully();
		defaultExecutorResource.close();
		defaultTimerResource.close();
	}

}
