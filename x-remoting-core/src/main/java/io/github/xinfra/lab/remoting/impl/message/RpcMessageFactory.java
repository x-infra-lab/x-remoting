package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.exception.ConnectionClosedException;
import io.github.xinfra.lab.remoting.exception.SendMessageException;
import io.github.xinfra.lab.remoting.exception.TimeoutException;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.impl.exception.RpcServerException;

import java.net.SocketAddress;

public class RpcMessageFactory implements MessageFactory {

	@Override
	public RemotingResponseMessage createSendFailedResponseMessage(int id, Throwable cause, SocketAddress remoteAddress) {
		RemotingResponseMessage rpcResponseMessage = new RemotingResponseMessage(id);
		rpcResponseMessage.setStatus(ResponseStatus.CLIENT_SEND_ERROR.getCode());
		rpcResponseMessage.setCause(new SendMessageException(cause));
		rpcResponseMessage.setRemoteAddress(remoteAddress);
		return rpcResponseMessage;
	}

	@Override
	public RemotingResponseMessage createTimeoutResponseMessage(int id, SocketAddress remoteAddress) {
		RemotingResponseMessage rpcResponseMessage = new RemotingResponseMessage(id);
		rpcResponseMessage.setStatus(ResponseStatus.TIMEOUT.getCode());
		rpcResponseMessage.setCause(new TimeoutException());
		rpcResponseMessage.setRemoteAddress(remoteAddress);
		return rpcResponseMessage;
	}

	@Override
	public RemotingRequestMessage createRequestMessage() {
		return new RemotingRequestMessage(IDGenerator.nextRequestId());
	}

	@Override
	public RemotingRequestMessage createHeartbeatRequestMessage() {
		return new RpcHeartbeatRequestMessage(IDGenerator.nextRequestId());
	}

	@Override
	public RemotingResponseMessage createExceptionResponse(int id, Throwable t, ResponseStatus status) {
		RemotingResponseMessage rpcResponseMessage = new RemotingResponseMessage(id);
		rpcResponseMessage.setStatus(status.getCode());
		RpcServerException rpcServerException = new RpcServerException(t);
		rpcResponseMessage.setContent(rpcServerException);
		rpcResponseMessage.setContentType(RpcServerException.class.getName());
		return rpcResponseMessage;
	}

	@Override
	public RemotingResponseMessage createExceptionResponse(int id, Throwable t, String errorMsg) {
		RemotingResponseMessage rpcResponseMessage = new RemotingResponseMessage(id);
		rpcResponseMessage.setStatus(ResponseStatus.SERVER_EXCEPTION.getCode());
		RpcServerException rpcServerException = new RpcServerException(errorMsg, t);
		rpcResponseMessage.setContent(rpcServerException);
		rpcResponseMessage.setContentType(RpcServerException.class.getName());
		return rpcResponseMessage;
	}

	@Override
	public RemotingResponseMessage createExceptionResponse(int id, String errorMsg) {
		RemotingResponseMessage rpcResponseMessage = new RemotingResponseMessage(id);
		rpcResponseMessage.setStatus(ResponseStatus.SERVER_EXCEPTION.getCode());
		RpcServerException rpcServerException = new RpcServerException(errorMsg);
		rpcResponseMessage.setContent(rpcServerException);
		rpcResponseMessage.setContentType(RpcServerException.class.getName());
		return rpcResponseMessage;
	}

	@Override
	public RemotingResponseMessage createResponse(int id, Object responseContent) {
		RemotingResponseMessage responseMessage = new RemotingResponseMessage(id);
		responseMessage.setStatus(ResponseStatus.SUCCESS.getCode());
		if (responseContent != null) {
			responseMessage.setContent(responseContent);
			responseMessage.setContentType(responseContent.getClass().getName());
		}
		return responseMessage;
	}

	@Override
	public RemotingResponseMessage createConnectionClosedMessage(int id, SocketAddress remoteAddress) {
		RemotingResponseMessage rpcResponseMessage = new RemotingResponseMessage(id);
		rpcResponseMessage.setStatus(ResponseStatus.CONNECTION_CLOSED.getCode());
		rpcResponseMessage.setCause(new ConnectionClosedException());
		rpcResponseMessage.setRemoteAddress(remoteAddress);
		return rpcResponseMessage;
	}

}
