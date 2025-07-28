package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.common.NamedThreadFactory;
import io.github.xinfra.lab.remoting.common.Resource;
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
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class DefaultConnectionFactory implements ConnectionFactory {

	private Protocol protocol;

	private Bootstrap bootstrap;

	private ConnectionConfig connectionConfig;

	// todo EpollUtils
	private final EventLoopGroup workerGroup = Epoll.isAvailable()
			? new EpollEventLoopGroup(Runtime.getRuntime().availableProcessors(),
					new NamedThreadFactory("RemotingClient-Client-IO-Worker"))
			: new NioEventLoopGroup(Runtime.getRuntime().availableProcessors(),
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

	private static final Class<? extends SocketChannel> channelClass = Epoll.isAvailable() ? EpollSocketChannel.class
			: NioSocketChannel.class;

	public DefaultConnectionFactory(Protocol protocol, List<Supplier<ChannelHandler>> channelHandlerSuppliers) {
		this(protocol, channelHandlerSuppliers, new ConnectionConfig());
	}

	// Q: why use Supplier to get ChannelHandler?
	// A: some ChannelHandler is not @ChannelHandler.Sharable. need create instance every
	// time
	public DefaultConnectionFactory(Protocol protocol, List<Supplier<ChannelHandler>> channelHandlerSuppliers,
			ConnectionConfig connectionConfig) {
		Validate.notNull(protocol, "protocol can not be null");
		Validate.notNull(channelHandlerSuppliers, "channelHandlers can not be null");
		Validate.notNull(connectionConfig, "connectionConfig can not be null");
		this.protocol = protocol;
		this.connectionConfig = connectionConfig;
		if (connectionConfig.getExecutor() != null) {
			this.executor = connectionConfig.getExecutor();
		}
		else {
			this.executor = defaultExecutorResource.get();
		}
		if (connectionConfig.getTimer() != null) {
			this.timer = connectionConfig.getTimer();
		}
		else {
			this.timer = defaultTimerResource.get();
		}

		bootstrap = new Bootstrap();
		bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
			.group(workerGroup)
			.channel(channelClass)
			.handler(new ChannelInitializer<SocketChannel>() {

				@Override
				protected void initChannel(SocketChannel ch) throws Exception {
					ChannelPipeline pipeline = ch.pipeline();
					if (connectionConfig.isIdleSwitch()) {
						pipeline.addLast("idleStateHandler",
								new IdleStateHandler(connectionConfig.getIdleReaderTimeout(),
										connectionConfig.getIdleWriterTimeout(), connectionConfig.getIdleAllTimeout(),
										TimeUnit.MILLISECONDS));
					}

					for (Supplier<ChannelHandler> supplier : channelHandlerSuppliers) {
						pipeline.addLast(supplier.get());
					}

					// todo FlushConsolidationHandler
				}
			});
	}

	@Override
	public Connection create(SocketAddress socketAddress) throws RemotingException {
		bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionConfig.getConnectTimeout());
		ChannelFuture future = bootstrap.connect(socketAddress);

		future.awaitUninterruptibly();
		if (!future.isDone()) {
			String errMsg = "Create connection to " + socketAddress + " timeout!";
			log.warn(errMsg);
			throw new RemotingException(errMsg);
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
