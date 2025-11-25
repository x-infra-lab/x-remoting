package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.message.ResponseMessage;

import java.util.concurrent.Executor;

public interface InvokeCallBack {

	void complete(ResponseMessage responseMessage);

	default Executor executor() {
		return null;
	}

}
