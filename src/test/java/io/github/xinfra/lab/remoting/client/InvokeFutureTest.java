package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.Endpoint;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageType;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.RpcProtocol;
import io.netty.channel.Channel;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InvokeFutureTest {
    private InvokeFuture invokeFuture;

    private InvokeFuture newInvokeFuture() {
        Endpoint endpoint = new Endpoint(ProtocolType.RPC, "localhost", 0);
        Channel channel = new EmbeddedChannel();
        Connection connection = new Connection(endpoint, channel);

        final int requestId1 = IDGenerator.nextRequestId();
        return new InvokeFuture(requestId1, connection);
    }

    @BeforeClass
    public static void beforeClass() {
        ProtocolManager.registerProtocolIfAbsent(ProtocolType.RPC, new RpcProtocol());
    }

    @Before
    public void before() {
        invokeFuture = newInvokeFuture();
    }

    @Test
    public void testTimeout() {
        Assert.assertNull(invokeFuture.getTimeout());
        Assert.assertFalse(invokeFuture.cancelTimeout());

        HashedWheelTimer timer = new HashedWheelTimer();

        Timeout timeout = timer.newTimeout(t -> {
        }, 3, TimeUnit.SECONDS);
        invokeFuture.addTimeout(timeout);
        Assert.assertEquals(invokeFuture.getTimeout(), timeout);

        Assert.assertTrue(invokeFuture.cancelTimeout());
        Assert.assertFalse(invokeFuture.cancelTimeout());
        Assert.assertTrue(invokeFuture.getTimeout().isCancelled());

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            invokeFuture.addTimeout(timeout);
        });
    }

    @Test
    public void testAwait() throws InterruptedException {
        Message message = mock(Message.class);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            invokeFuture.finish(message);
        });

        Assert.assertNull(invokeFuture.await(1, TimeUnit.SECONDS));
        Assert.assertFalse(invokeFuture.isDone());

        Message result = invokeFuture.await();
        Assert.assertSame(result, message);
        Assert.assertTrue(invokeFuture.isDone());

        result = invokeFuture.await();
        Assert.assertSame(result, message);
        Assert.assertTrue(invokeFuture.isDone());
    }

    @Test
    public void testAppClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Assert.assertSame(contextClassLoader, invokeFuture.getAppClassLoader());

        try {
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{}, contextClassLoader);
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            InvokeFuture future = newInvokeFuture();
            Assert.assertSame(future.getAppClassLoader(), urlClassLoader);
        } finally {
            // recover current thread context classLoader
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }

    }

    @Test
    public void testCallBackSync() {
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        AtomicInteger callBackExecuteTimes = new AtomicInteger(0);
        InvokeCallBack callBack = future -> {
            callbackExecuted.set(true);
            callBackExecuteTimes.getAndIncrement();
        };
        invokeFuture.addCallBack(callBack);

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            invokeFuture.addCallBack(callBack);
        });

        Message message = mock(Message.class);
        when(message.protocolType()).thenReturn(ProtocolType.RPC);

        invokeFuture.finish(message);

        invokeFuture.executeCallBack();
        Assert.assertTrue(callbackExecuted.get());
        Assert.assertEquals(1, callBackExecuteTimes.get());

        invokeFuture.executeCallBack();
        Assert.assertTrue(callbackExecuted.get());
        Assert.assertEquals(1, callBackExecuteTimes.get());
    }

    @Test
    public void testCallBackAsync() throws InterruptedException {
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        AtomicInteger callBackExecuteTimes = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        InvokeCallBack callBack = future -> {
            callbackExecuted.set(true);
            callBackExecuteTimes.getAndIncrement();
            countDownLatch.countDown();
        };
        invokeFuture.addCallBack(callBack);

        Assert.assertThrows(IllegalArgumentException.class, () -> {
            invokeFuture.addCallBack(callBack);
        });

        Message message = mock(Message.class);
        when(message.protocolType()).thenReturn(ProtocolType.RPC);

        invokeFuture.finish(message);

        invokeFuture.asyncExecuteCallBack();
        countDownLatch.await();
        Assert.assertTrue(callbackExecuted.get());
        Assert.assertEquals(1, callBackExecuteTimes.get());

        invokeFuture.asyncExecuteCallBack();
        countDownLatch.await();
        Assert.assertTrue(callbackExecuted.get());
        Assert.assertEquals(1, callBackExecuteTimes.get());
    }

    @Test
    public void testCreateConnectionClosedMessage() {
        Message message = invokeFuture.createConnectionClosedMessage();
        Assert.assertNotNull(message);
        Assert.assertEquals(message.messageType(), MessageType.response);
    }
}
