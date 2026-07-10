package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.rpc.client.InFlightRequests;
import io.github.xinfra.lab.remoting.rpc.client.InvokeFuture;
import io.github.xinfra.lab.remoting.rpc.protocol.RpcProtocol;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@Slf4j
public class RpcMessageDispatcher implements MessageHandler {

	private final RemotingRequestMessageHandler requestMessageHandler;

	public RpcMessageDispatcher(RemotingRequestMessageHandler requestMessageHandler) {
		this.requestMessageHandler = requestMessageHandler;
	}

	@Override
	public void handleMessage(ChannelHandlerContext ctx, io.github.xinfra.lab.remoting.message.Message transportMsg) {
		Message msg = (Message) transportMsg;
		Connection connection = ctx.channel().attr(CONNECTION).get();
		if (connection == null) {
			log.warn("no Connection attribute on channel {}, dropping message id:{}", ctx.channel().remoteAddress(),
					msg.getId());
			return;
		}
		try {
			MessageType messageType = msg.getMessageType();
			if (messageType == MessageType.heartbeatRequest) {
				handleHeartbeat(connection, (RequestMessage) msg);
			}
			else if (messageType == MessageType.response) {
				handleResponse(connection, (ResponseMessage) msg);
			}
			else if (messageType == MessageType.request) {
				requestMessageHandler.handleMessage(connection, (RequestMessage) msg);
			}
			else if (messageType == MessageType.goaway) {
				handleGoaway(connection);
			}
			else {
				log.warn("Unknown message type: {}", messageType);
			}
		}
		catch (Exception e) {
			log.error("handleMessage failed. id:{} messageType:{} remoteAddress:{}", msg.getId(), msg.getMessageType(),
					connection.remoteAddress(), e);
			Responses.handleExceptionResponse(connection, msg, e);
		}
	}

	private void handleGoaway(Connection connection) {
		log.info("received GOAWAY from {}, closing connection", connection.remoteAddress());
		connection.getChannel().attr(Connection.GOAWAY).set(Boolean.TRUE);
		connection.close();
	}

	private void handleHeartbeat(Connection connection, RequestMessage requestMessage) {
		RpcProtocol protocol = (RpcProtocol) connection.getProtocol();
		ResponseMessage responseMessage = protocol.getMessageFactory()
			.createResponse(requestMessage.getId(), requestMessage.getSerializationType(), ResponseStatus.OK);
		Responses.sendResponse(connection, responseMessage);
	}

	private void handleResponse(Connection connection, ResponseMessage responseMessage) {
		int id = responseMessage.getId();
		InFlightRequests inFlight = InFlightRequests.of(connection);
		if (inFlight == null) {
			log.warn("no InFlightRequests for response id:{} from:{}", id, connection.remoteAddress());
			return;
		}
		InvokeFuture<?> future = inFlight.remove(id);
		if (future != null) {
			future.cancelTimeout();
			future.complete(responseMessage);
			try {
				future.executeCallBack(connection.getExecutor());
			}
			catch (Throwable t) {
				log.error("executeCallBack fail. getId:{}", responseMessage.getId(), t);
			}
		}
		else {
			log.warn("can not find InvokeFuture maybe timeout. getId:{} message:{} from:{}", responseMessage.getId(),
					responseMessage, connection.remoteAddress());
		}
	}

}
