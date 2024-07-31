package io.github.xinfra.lab.remoting.rpc.heartbeat;

import io.github.xinfra.lab.remoting.client.BaseRemoting;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.heartbeat.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.rpc.message.ResponseStatus;
import io.github.xinfra.lab.remoting.rpc.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.rpc.message.RpcResponseMessage;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

import static io.github.xinfra.lab.remoting.connection.Connection.CONNECTION;
import static io.github.xinfra.lab.remoting.connection.Connection.HEARTBEAT_FAIL_COUNT;

@Slf4j
public class RpcHeartbeatTrigger implements HeartbeatTrigger {
    private MessageFactory messageFactory;

    private int heartbeatTimeoutMills = 1000;

    private int maxFailCount = 3;

    private BaseRemoting baseRemoting;


    public RpcHeartbeatTrigger(RpcMessageFactory messageFactory) {
        this.messageFactory = messageFactory;
        baseRemoting = new BaseRemoting(messageFactory);
        baseRemoting.startup();
    }

    @Override
    public void triggerHeartBeat(ChannelHandlerContext ctx) {
        Message heartbeatRequestMessage = messageFactory.createHeartbeatRequestMessage();
        Connection connection = ctx.channel().attr(CONNECTION).get();
        Integer heartbeatFailCount = ctx.channel().attr(HEARTBEAT_FAIL_COUNT).get();

        if (heartbeatFailCount > maxFailCount) {
            connection.close();
            log.error("close connection after heartbeat fail {} times", heartbeatFailCount);
            return;
        }

        baseRemoting.asyncCall(heartbeatRequestMessage, connection, heartbeatTimeoutMills,
                message -> {
                    RpcResponseMessage heartbeatResponseMessage = (RpcResponseMessage) message;
                    SocketAddress remoteAddress = heartbeatResponseMessage.getRemoteAddress();

                    if (heartbeatResponseMessage.getStatus() == ResponseStatus.SUCCESS.getCode()) {
                        log.debug("heartbeat success. remote address:{}", remoteAddress);
                        ctx.channel().attr(HEARTBEAT_FAIL_COUNT).set(0);
                    } else {
                        Integer failCount = ctx.channel().attr(HEARTBEAT_FAIL_COUNT).get();
                        log.warn("heartbeat fail {} times. remote address:{}", failCount, remoteAddress);
                        ctx.channel().attr(HEARTBEAT_FAIL_COUNT).set(failCount + 1);
                    }

                }
        );


    }
}
