package io.github.xinfra.lab.remoting.benchmark;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.rpc.client.CallOptions;
import io.github.xinfra.lab.remoting.rpc.client.RemotingClient;
import io.github.xinfra.lab.remoting.rpc.client.RemotingFuture;
import io.github.xinfra.lab.remoting.rpc.handler.BlockingRequestHandler;
import io.github.xinfra.lab.remoting.rpc.handler.EchoRequest;
import io.github.xinfra.lab.remoting.rpc.server.RemotingServer;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 3)
@Measurement(iterations = 5, time = 5)
@Fork(1)
@State(Scope.Benchmark)
public class RpcBenchmark {

	private static final String ECHO_PATH = "/echo";

	private RemotingServer server;

	private RemotingClient client;

	private InetSocketAddress serverAddress;

	private CallOptions callOptions;

	private EchoRequest request;

	@Setup(Level.Trial)
	public void setup() {
		server = new RemotingServer();
		server.registerRequestHandler(ECHO_PATH,
				BlockingRequestHandler.of((EchoRequest req) -> "echo:" + req.getMsg()));
		server.startup();
		serverAddress = server.getLocalAddress();

		client = new RemotingClient();
		client.startup();

		callOptions = CallOptions.defaults();
		request = new EchoRequest("benchmark");
	}

	@TearDown(Level.Trial)
	public void teardown() {
		client.shutdown();
		server.shutdown();
	}

	@Benchmark
	public String blockingCall() throws RemotingException, InterruptedException {
		return client.blockingCall(ECHO_PATH, request, serverAddress, callOptions);
	}

	@Benchmark
	public String futureCall() throws RemotingException, InterruptedException, TimeoutException {
		RemotingFuture<String> future = client.futureCall(ECHO_PATH, request, serverAddress, callOptions);
		return future.get(callOptions.getTimeoutMills(), TimeUnit.MILLISECONDS);
	}

	@Benchmark
	public void onewayCall() throws RemotingException {
		client.oneway(ECHO_PATH, request, serverAddress, callOptions);
	}

	public static void main(String[] args) throws Exception {
		Options opts = new OptionsBuilder().include(RpcBenchmark.class.getSimpleName()).build();
		new Runner(opts).run();
	}

}
