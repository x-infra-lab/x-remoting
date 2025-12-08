package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.message.ResponseMessage;

import java.util.concurrent.Executor;

public interface InvokeCallBack {

	void onMessage(ResponseMessage responseMessage);

	default Executor getExecutor() {
		return null;
	}

}
