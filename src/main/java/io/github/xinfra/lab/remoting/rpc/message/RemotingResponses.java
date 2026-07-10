package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class RemotingResponses {

	private RemotingResponses() {
	}

	public static <R> R getResponseObject(RemotingResponseMessage remotingResponseMessage) throws RemotingException {
		remotingResponseMessage.deserialize();
		ResponseStatus responseStatus = remotingResponseMessage.getResponseStatus();
		RemotingMessageBody body = remotingResponseMessage.getBody();
		Object bodyValue = body == null ? null : body.getBodyValue();
		if (Objects.equals(responseStatus, ResponseStatus.OK)) {
			return (R) bodyValue;
		}

		if (bodyValue instanceof Throwable) {
			throw new RemotingException("remoting invoke fail. ", (Throwable) body.getBodyValue());
		}
		else {
			throw new RemotingException("remoting invoke fail. unknown: " + body.getBodyValue());
		}
	}

}
