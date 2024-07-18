package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ProtocolHandlerTest {

    @Test
    public void testProtocolHandler() {
        ProtocolType testProtocol = new ProtocolType("testProtocolHandler", "testProtocolHandler".getBytes());
        ProtocolManager.registerProtocolIfAbsent(testProtocol, new TestProtocol());
        ProtocolHandler protocolHandler = new ProtocolHandler(new ConcurrentHashMap<>());

        EmbeddedChannel channel = new EmbeddedChannel();
        channel.pipeline().addLast(protocolHandler);
        channel.attr(Connection.PROTOCOL).set(testProtocol);

        MessageHandler messageHandler = new MessageHandler() {

            @Override
            public Executor executor() {
                return null;
            }

            @Override
            public void handleMessage(RemotingContext remotingContext, Object msg) {

            }
        };

        MessageHandler spyMessageHandler = spy(messageHandler);
        Protocol protocol = ProtocolManager.getProtocol(testProtocol);
        ((TestProtocol) protocol).setTestMessageHandler(spyMessageHandler);

        Object object = new Object();
        channel.writeInbound(object);
        verify(spyMessageHandler, times(0)).handleMessage(any(), any());
        Object inboundMessage = channel.inboundMessages().poll();
        Assert.assertTrue(object == inboundMessage);


        Message message = mock(Message.class);
        channel.writeInbound(message);
        verify(spyMessageHandler, times(1)).handleMessage(any(), any());
    }
}
