package io.github.xinfra.lab.remoting.rpc.client;

import io.github.xinfra.lab.remoting.rpc.message.ResponseMessage;

import java.util.concurrent.Executor;

public interface InvokeCallBack {

	void onMessage(ResponseMessage responseMessage);

	default Executor getExecutor() {
		return null;
	}

}
