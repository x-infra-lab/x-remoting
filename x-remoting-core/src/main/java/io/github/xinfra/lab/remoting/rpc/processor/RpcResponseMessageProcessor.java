package io.github.xinfra.lab.remoting.rpc.processor;

import io.github.xinfra.lab.remoting.rpc.message.MessageHandlerContext;
import io.github.xinfra.lab.remoting.client.InvokeFuture;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessage;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RpcResponseMessageProcessor extends AbstractMessageProcessor<RpcMessage> {

	@Override
	public void handleMessage(MessageHandlerContext messageHandlerContext, RpcMessage message) {
		ProcessTask processTask = new ProcessTask(messageHandlerContext, (RpcResponseMessage) message);
		if (executor() != null) {
			executor().submit(processTask);
		}
		else {
			messageHandlerContext.getMessageDefaultExecutor().execute(processTask);
		}
	}

	private void doProcess(MessageHandlerContext messageHandlerContext, RpcResponseMessage responseMessage) {
		int id = responseMessage.id();
		Connection connection = messageHandlerContext.getConnection();
		InvokeFuture<?> future = connection.removeInvokeFuture(id);
		if (future != null) {
			future.cancelTimeout();
			future.complete(responseMessage);
			try {
				future.executeCallBack();
			}
			catch (Throwable t) {
				log.error("executeCallBack fail. id:{}", responseMessage.id(), t);
				throw t;
			}
		}
		else {
			log.warn("can not find InvokeFuture maybe timeout. id:{} status:{} from:{}", responseMessage.id(),
					responseMessage.getStatus(), connection.remoteAddress());
		}
	}

	class ProcessTask implements Runnable {

		private MessageHandlerContext messageHandlerContext;

		private RpcResponseMessage responseMessage;

		public ProcessTask(MessageHandlerContext messageHandlerContext, RpcResponseMessage responseMessage) {
			this.messageHandlerContext = messageHandlerContext;
			this.responseMessage = responseMessage;
		}

		@Override
		public void run() {
			try {
				doProcess(messageHandlerContext, responseMessage);
			}
			catch (Throwable t) {
				log.error("process response fail. id:{}, status:{} from:{}", responseMessage.id(),
						responseMessage.getStatus(), messageHandlerContext.getConnection().remoteAddress(), t);
			}
		}

	}

}
