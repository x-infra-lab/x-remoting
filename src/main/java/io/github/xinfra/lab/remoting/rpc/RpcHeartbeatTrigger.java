package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.client.BaseRemoting;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.HeartbeatTrigger;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageFactory;
import io.github.xinfra.lab.remoting.message.ResponseStatus;
import io.github.xinfra.lab.remoting.message.RpcMessageFactory;
import io.github.xinfra.lab.remoting.message.RpcResponseMessage;
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


    public RpcHeartbeatTrigger(RpcMessageFactory messageFactory) {
        this.messageFactory = messageFactory;
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

        BaseRemoting baseRemoting = new BaseRemoting(messageFactory);
        baseRemoting.asyncCall(heartbeatRequestMessage, connection, heartbeatTimeoutMills,
                future -> {

                    RpcResponseMessage heartbeatResponseMessage;
                    SocketAddress remoteAddress = future.getConnection().getChannel().remoteAddress();
                    try {
                        heartbeatResponseMessage = (RpcResponseMessage) future.await();
                    } catch (InterruptedException e) {
                        log.error("fail get response from InvokeFuture. remote address:{}", remoteAddress, e);
                        return;
                    }

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
