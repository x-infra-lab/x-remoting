package io.github.xinfra.lab.remoting.common;

import javax.net.ServerSocketFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;

public class TestSocketUtils {

	/**
	 * The minimum value for port ranges used when finding an available TCP port.
	 */
	private static final int PORT_RANGE_MIN = 1024;

	/**
	 * The maximum value for port ranges used when finding an available TCP port.
	 */
	private static final int PORT_RANGE_MAX = 65535;

	private static final int PORT_RANGE = PORT_RANGE_MAX - PORT_RANGE_MIN;

	private static final int MAX_ATTEMPTS = 1_000;

	private static final Random random = new Random(System.nanoTime());

	/**
	 * Find an available TCP port randomly selected from the range [1024, 65535].
	 * @return an available TCP port number
	 * @throws IllegalStateException if no available port could be found
	 */
	public static int findAvailableTcpPort() {
		int candidatePort;
		int searchCounter = 0;
		do {
			if (searchCounter > MAX_ATTEMPTS) {
				throw new IllegalStateException(
						String.format("Could not find an available TCP port in the range [%d, %d] after %d attempts",
								PORT_RANGE_MIN, PORT_RANGE_MAX, MAX_ATTEMPTS));
			}
			candidatePort = PORT_RANGE_MIN + random.nextInt(PORT_RANGE + 1);
			searchCounter++;
		}
		while (!isPortAvailable(candidatePort));

		return candidatePort;
	}

	/**
	 * Determine if the specified TCP port is currently available on {@code localhost}.
	 */
	private static boolean isPortAvailable(int port) {
		try {
			ServerSocket serverSocket = ServerSocketFactory.getDefault()
				.createServerSocket(port, 1, InetAddress.getByName("localhost"));
			serverSocket.close();
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	}

}
