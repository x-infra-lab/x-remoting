package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.rpc.message.RemotingResponseMessage;
import io.github.xinfra.lab.remoting.rpc.message.RemotingResponses;
import io.github.xinfra.lab.remoting.rpc.message.ResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface RemotingCallBack<R> extends InvokeCallBack {

	Logger LOGGER = LoggerFactory.getLogger(RemotingCallBack.class);

	@Override
	default void onMessage(ResponseMessage responseMessage) {
		try {
			RemotingResponseMessage remotingResponseMessage = (RemotingResponseMessage) responseMessage;
			Object responseObject = RemotingResponses.getResponseObject(remotingResponseMessage);
			try {
				onResponse((R) responseObject);
			}
			catch (Throwable t) {
				LOGGER.error("call back execute onResponse fail.", t);
			}
		}
		catch (Throwable t) {
			try {
				onException(t);
			}
			catch (Throwable throwable) {
				LOGGER.error("call back execute onException fail.", throwable);
			}
		}
	}

	void onException(Throwable t);

	void onResponse(R response);

}
