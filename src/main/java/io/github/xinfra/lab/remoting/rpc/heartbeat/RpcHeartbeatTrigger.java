package io.github.xinfra.lab.remoting.rpc.heartbeat;

import io.github.xinfra.lab.remoting.client.BaseRemoting;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.heartbeat.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.net.SocketAddress;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;

@Slf4j
public class RpcHeartbeatTrigger implements HeartbeatTrigger {

	private int heartbeatTimeoutMills = 1000;

	private int maxFailCount = 3;

	private volatile BaseRemoting baseRemoting;

	public RpcHeartbeatTrigger() {
	}

	@Override
	public void triggerHeartBeat(ChannelHandlerContext ctx) {
		Connection connection = ctx.channel().attr(CONNECTION).get();
		int heartbeatFailCount = connection.getHeartbeatFailCnt();
		if (heartbeatFailCount > maxFailCount) {
			connection.close();
			log.error("close connection after heartbeat fail {} times. remote address:{}", heartbeatFailCount,
					connection.remoteAddress());
			return;
		}

		Protocol protocol = connection.getProtocol();
		if (baseRemoting == null) {
			synchronized (this) {
				if (baseRemoting == null) {
					this.baseRemoting = new BaseRemoting(protocol);
				}
			}
		}

		Message heartbeatRequestMessage = protocol.messageFactory().createHeartbeatRequestMessage();
		baseRemoting.asyncCall(heartbeatRequestMessage, connection, heartbeatTimeoutMills, message -> {
			RpcResponseMessage heartbeatResponseMessage = (RpcResponseMessage) message;
			SocketAddress remoteAddress = heartbeatResponseMessage.getRemoteAddress();

			if (heartbeatResponseMessage.getStatus() == ResponseStatus.SUCCESS.getCode()) {
				log.debug("heartbeat success. remote address:{}", remoteAddress);
				connection.setHeartbeatFailCnt(0);
			}
			else {
				int failCount = connection.getHeartbeatFailCnt() + 1;
				log.warn("heartbeat fail {} times. remote address:{}", failCount, remoteAddress);
				connection.setHeartbeatFailCnt(failCount);
			}

		});

	}

	@Override
	public void setHeartbeatMaxFailCount(int failCount) {
		Validate.isTrue(failCount >= 0, "failCount must >= 0");
		this.maxFailCount = failCount;
	}

	@Override
	public void setHeartbeatTimeoutMills(int timeoutMills) {
		Validate.isTrue(timeoutMills > 0, "timeoutMills must > 0");
		this.heartbeatTimeoutMills = timeoutMills;
	}

}
