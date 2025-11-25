package io.github.xinfra.lab.remoting.impl.message;

import io.github.xinfra.lab.remoting.exception.RemotingException;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class RemotingResponses {

	private RemotingResponses() {
	}

	public static <R> R getResponseObject(RemotingResponseMessage remotingResponseMessage) throws RemotingException {
		// todo: fix classloader problem
		remotingResponseMessage.deserialize();
		ResponseStatus responseStatus = remotingResponseMessage.responseStatus();
		RemotingMessageBody body = remotingResponseMessage.body();
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
