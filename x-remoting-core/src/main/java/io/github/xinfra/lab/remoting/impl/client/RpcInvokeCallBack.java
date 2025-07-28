package io.github.xinfra.lab.remoting.impl.client;

import io.github.xinfra.lab.remoting.client.InvokeCallBack;
import io.github.xinfra.lab.remoting.message.ResponseMessage;
import io.github.xinfra.lab.remoting.impl.message.RpcResponses;
import io.github.xinfra.lab.remoting.impl.message.RemotingResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

public interface RpcInvokeCallBack<R> extends InvokeCallBack {

	Logger LOGGER = LoggerFactory.getLogger(RpcInvokeCallBack.class);

	@Override
	default void complete(ResponseMessage responseMessage) {
		Runnable task = () -> {
			try {
				RemotingResponseMessage rpcResponseMessage = (RemotingResponseMessage) responseMessage;
				Object responseObject = RpcResponses.getResponseObject(rpcResponseMessage);
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
		};

		Executor executor = this.executor();

		if (executor != null) {
			try {
				executor.execute(task);
			}
			catch (RejectedExecutionException re) {
				LOGGER.error("fail execute callback. id:{}", rpcResponseMessage.id(), re);
			}
		}
		else {
			task.run();
		}
	}

	void onException(Throwable t);

	void onResponse(R response);

}