// todo @joecqupt fix it : move to [x-remoting-api] module
//public class RpcHeartBeatTest {
//
//	private RemotingServer remotingServer;
//
//	@BeforeEach
//	public void before() {
//		RemotingServerConfig remotingServerConfig = new RemotingServerConfig();
//		remotingServerConfig.setPort(findAvailableTcpPort());
//		remotingServer = new RemotingServer(remotingServerConfig);
//		remotingServer.startup();
//	}
//
//	@AfterEach
//	public void after() {
//		remotingServer.shutdown();
//	}
//
//	@Test
//	public void heartbeatTest1() throws RemotingException, InterruptedException, IOException {
//		SocketAddress remoteAddress = remotingServer.localAddress();
//
//		Protocol protocol = new RemotingProtocol(new RequestHandlerRegistry());
//		ConnectionManager connectionManager = new ClientConnectionManager(protocol);
//		connectionManager.startup();
//
//		MessageFactory messageFactory = protocol.messageFactory();
//		Call call = new Call();
//		Message heartbeatRequestMessage = messageFactory.createHeartbeatRequestMessage();
//
//		Connection connection = connectionManager.get(remoteAddress);
//
//		Message heartbeatResponseMessage = call.blockingCall(heartbeatRequestMessage, connection, 1000);
//
//		Assertions.assertNotNull(heartbeatResponseMessage);
//
//		CountDownLatch countDownLatch = new CountDownLatch(1);
//		AtomicReference<Message> messageAtomicReference = new AtomicReference<>();
//		call.asyncCall(heartbeatRequestMessage, connection, 1000, message -> {
//			messageAtomicReference.set(message);
//			countDownLatch.countDown();
//		});
//
//		countDownLatch.await(3, TimeUnit.SECONDS);
//		Assertions.assertNotNull(messageAtomicReference.get());
//
//		connectionManager.shutdown();
//		protocol.close();
//	}
//
//}
