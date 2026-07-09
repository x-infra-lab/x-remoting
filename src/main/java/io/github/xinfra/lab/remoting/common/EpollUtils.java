package io.github.xinfra.lab.remoting.common;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.ThreadFactory;

/**
 * Centralises the Epoll-vs-Nio transport selection used by both the client
 * ({@link io.github.xinfra.lab.remoting.connection.DefaultConnectionFactory}) and the
 * server ({@link io.github.xinfra.lab.remoting.server.AbstractServer}).
 */
public final class EpollUtils {

	private static final boolean EPOLL_AVAILABLE = Epoll.isAvailable();

	private EpollUtils() {
	}

	public static boolean isEpollAvailable() {
		return EPOLL_AVAILABLE;
	}

	public static EventLoopGroup newEventLoopGroup(int threads, ThreadFactory threadFactory) {
		return EPOLL_AVAILABLE ? new EpollEventLoopGroup(threads, threadFactory)
				: new NioEventLoopGroup(threads, threadFactory);
	}

	public static Class<? extends SocketChannel> clientChannelClass() {
		return EPOLL_AVAILABLE ? EpollSocketChannel.class : NioSocketChannel.class;
	}

	public static Class<? extends ServerChannel> serverChannelClass() {
		return EPOLL_AVAILABLE ? EpollServerSocketChannel.class : NioServerSocketChannel.class;
	}

}
