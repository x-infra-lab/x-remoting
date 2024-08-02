package io.github.xinfra.lab.remoting.connection;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ProtocolHandlerTest {
    TestProtocol testProtocol = new TestProtocol();
    @Test
    public void testProtocolHandler() {
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
        testProtocol.setTestMessageHandler(spyMessageHandler);

        Object object = new Object();
        channel.writeInbound(object);
        verify(spyMessageHandler, times(0)).handleMessage(any(), any());
        Object inboundMessage = channel.inboundMessages().poll();
        Assertions.assertTrue(object == inboundMessage);


        Message message = mock(Message.class);
        channel.writeInbound(message);
        verify(spyMessageHandler, times(1)).handleMessage(any(), any());
    }
}
