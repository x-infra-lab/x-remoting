package io.github.xinfra.lab.remoting.rpc.message;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.common.Wait;
import io.github.xinfra.lab.remoting.exception.DeserializeException;
import io.github.xinfra.lab.remoting.exception.SerializeException;
import io.github.xinfra.lab.remoting.processor.UserProcessor;
import io.github.xinfra.lab.remoting.rpc.exception.RpcServerException;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class RpcMessageHandlerTest {

    static class EchoProcessor implements UserProcessor<String> {

        @Override
        public String interest() {
            return String.class.getName();
        }

        @Override
        public Object handRequest(String request) {
            // echo request
            return "echo:" + request;
        }
    }

    @Test
    public void testHandleRequest() throws SerializeException, InterruptedException, TimeoutException {
        // build a requestMessage
        String content = "this is rpc content";
        String contentType = content.getClass().getName();
        RpcMessageHeader header = new RpcMessageHeader();
        header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
        Integer requestId = IDGenerator.nextRequestId();
        RpcRequestMessage requestMessage = new RpcRequestMessage(requestId);
        requestMessage.setHeader(header);
        requestMessage.setContent(content);
        requestMessage.setContentType(contentType);
        requestMessage.serialize();
        requestMessage.setHeader(null);
        requestMessage.setContentType(null);
        requestMessage.setContent(null);

        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();
        RpcMessageHandler messageHandler = new RpcMessageHandler(rpcMessageFactory);

        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        EmbeddedChannel channel = new EmbeddedChannel();
        doReturn(channel).when(context).channel();
        doReturn(channel.newSucceededFuture()).when(context).writeAndFlush(any());

        EchoProcessor echoProcessor = new EchoProcessor();
        echoProcessor = spy(echoProcessor);
        ConcurrentHashMap<String, UserProcessor<?>> userProcessors = new ConcurrentHashMap<>();
        userProcessors.put(echoProcessor.interest(), echoProcessor);

        RemotingContext remotingContext = new RemotingContext(context, userProcessors);
        messageHandler.handleMessage(remotingContext, requestMessage);

        Wait.untilIsTrue(() -> {
            try {
                verify(context, atLeastOnce()).writeAndFlush(any());
                return true;
            } catch (Throwable t) {
                return false;
            }
        }, 30, 100);

        verify(echoProcessor, times(1)).handRequest(eq(content));
        // verify response
        verify(context, times(1)).writeAndFlush(argThat(
                new ArgumentMatcher<RpcResponseMessage>() {
                    @Override
                    public boolean matches(RpcResponseMessage argument) {
                        if (argument.getStatus() != ResponseStatus.SUCCESS.getCode()) {
                            return false;
                        }
                        if (!Objects.equals(argument.getContentType(), String.class.getName())) {
                            return false;
                        }
                        if (!Objects.equals(argument.getContent(), "echo:" + content)) {
                            return false;
                        }
                        return true;
                    }
                }
        ));
    }

    @Test
    public void testHandleRequestDeserializeFailed() throws SerializeException, InterruptedException, TimeoutException, DeserializeException {
        // build a requestMessage
        String content = "this is rpc content";
        String contentType = content.getClass().getName();
        RpcMessageHeader header = new RpcMessageHeader();
        header.addItem(new RpcMessageHeader.Item("this is header key", "this is header value"));
        Integer requestId = IDGenerator.nextRequestId();
        RpcRequestMessage requestMessage = new RpcRequestMessage(requestId);
        requestMessage.setHeader(header);
        requestMessage.setContent(content);
        requestMessage.setContentType(contentType);
        requestMessage.serialize();
        requestMessage.setHeader(null);
        requestMessage.setContentType(null);
        requestMessage.setContent(null);

        requestMessage = spy(requestMessage);

        doThrow(new RuntimeException("deserialize exception")).when(requestMessage).deserialize(any());


        RpcMessageFactory rpcMessageFactory = new RpcMessageFactory();
        RpcMessageHandler messageHandler = new RpcMessageHandler(rpcMessageFactory);

        ChannelHandlerContext context = mock(ChannelHandlerContext.class);
        EmbeddedChannel channel = new EmbeddedChannel();
        doReturn(channel).when(context).channel();
        doReturn(channel.newSucceededFuture()).when(context).writeAndFlush(any());

        EchoProcessor echoProcessor = new EchoProcessor();
        echoProcessor = spy(echoProcessor);
        ConcurrentHashMap<String, UserProcessor<?>> userProcessors = new ConcurrentHashMap<>();
        userProcessors.put(echoProcessor.interest(), echoProcessor);

        RemotingContext remotingContext = new RemotingContext(context, userProcessors);
        messageHandler.handleMessage(remotingContext, requestMessage);

        Wait.untilIsTrue(() -> {
            try {
                verify(context, atLeastOnce()).writeAndFlush(any());
                return true;
            } catch (Throwable t) {
                return false;
            }
        }, 30, 100);

        verify(echoProcessor, times(0)).handRequest(eq(content));
        // verify response
        verify(context, times(1)).writeAndFlush(argThat(
                new ArgumentMatcher<RpcResponseMessage>() {
                    @Override
                    public boolean matches(RpcResponseMessage argument) {
                        if (argument.getStatus() != ResponseStatus.SERVER_DESERIAL_EXCEPTION.getCode()) {
                            return false;
                        }
                        if (argument.getCause() != null) {
                            return false;
                        }
                        if (!Objects.equals(argument.getContentType(), RpcServerException.class.getName())) {
                            return false;
                        }
                        if (!(argument.getContent() instanceof RpcServerException)) {
                            return false;
                        }
                        return true;
                    }
                }
        ));
    }
}
