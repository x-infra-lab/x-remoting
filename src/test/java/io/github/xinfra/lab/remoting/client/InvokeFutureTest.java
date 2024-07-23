package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageHandler;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.github.xinfra.lab.remoting.protocol.TestProtocol;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
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
        final int requestId1 = IDGenerator.nextRequestId();
        return new InvokeFuture(requestId1);
    }


    @BeforeEach
    public void before() {
        invokeFuture = newInvokeFuture();
    }

    @Test
    public void testTimeout() {
        Assertions.assertNull(invokeFuture.getTimeout());
        Assertions.assertFalse(invokeFuture.cancelTimeout());

        HashedWheelTimer timer = new HashedWheelTimer();

        Timeout timeout = timer.newTimeout(t -> {
        }, 3, TimeUnit.SECONDS);
        invokeFuture.addTimeout(timeout);
        Assertions.assertEquals(invokeFuture.getTimeout(), timeout);

        Assertions.assertTrue(invokeFuture.cancelTimeout());
        Assertions.assertFalse(invokeFuture.cancelTimeout());
        Assertions.assertTrue(invokeFuture.getTimeout().isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
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

        Assertions.assertNull(invokeFuture.await(1, TimeUnit.SECONDS));
        Assertions.assertFalse(invokeFuture.isDone());

        Message result = invokeFuture.await();
        Assertions.assertSame(result, message);
        Assertions.assertTrue(invokeFuture.isDone());

        result = invokeFuture.await();
        Assertions.assertSame(result, message);
        Assertions.assertTrue(invokeFuture.isDone());
    }

    @Test
    public void testAppClassLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        Assertions.assertSame(contextClassLoader, invokeFuture.getAppClassLoader());

        try {
            URLClassLoader urlClassLoader = new URLClassLoader(new URL[]{}, contextClassLoader);
            Thread.currentThread().setContextClassLoader(urlClassLoader);

            InvokeFuture future = newInvokeFuture();
            Assertions.assertSame(future.getAppClassLoader(), urlClassLoader);
        } finally {
            // recover current thread context classLoader
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }

    }

    @Test
    public void testCallBackSync() {
        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        AtomicInteger callBackExecuteTimes = new AtomicInteger(0);
        InvokeCallBack callBack = message -> {
            callbackExecuted.set(true);
            callBackExecuteTimes.getAndIncrement();
        };
        invokeFuture.addCallBack(callBack);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            invokeFuture.addCallBack(callBack);
        });

        Message message = mock(Message.class);

        invokeFuture.finish(message);

        invokeFuture.executeCallBack();
        Assertions.assertTrue(callbackExecuted.get());
        Assertions.assertEquals(1, callBackExecuteTimes.get());

        invokeFuture.executeCallBack();
        Assertions.assertTrue(callbackExecuted.get());
        Assertions.assertEquals(1, callBackExecuteTimes.get());
    }

    @Test
    public void testCallBackAsync() throws InterruptedException {
        ProtocolType test = new ProtocolType("testCallBackAsync", "testCallBackAsync".getBytes());
        ProtocolManager.registerProtocolIfAbsent(test, new TestProtocol() {
            @Override
            public MessageHandler messageHandler() {
                return new MessageHandler() {
                    @Override
                    public Executor executor() {
                        return Executors.newSingleThreadExecutor();
                    }

                    @Override
                    public void handleMessage(RemotingContext remotingContext, Object msg) {

                    }
                };
            }
        });

        AtomicBoolean callbackExecuted = new AtomicBoolean(false);
        AtomicInteger callBackExecuteTimes = new AtomicInteger(0);
        CountDownLatch countDownLatch = new CountDownLatch(1);
        InvokeCallBack callBack = message -> {
            callbackExecuted.set(true);
            callBackExecuteTimes.getAndIncrement();
            countDownLatch.countDown();
        };
        invokeFuture.addCallBack(callBack);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            invokeFuture.addCallBack(callBack);
        });

        Message message = mock(Message.class);
        when(message.protocolType()).thenReturn(test);

        invokeFuture.finish(message);

        invokeFuture.asyncExecuteCallBack();
        countDownLatch.await();
        Assertions.assertTrue(callbackExecuted.get());
        Assertions.assertEquals(1, callBackExecuteTimes.get());

        invokeFuture.asyncExecuteCallBack();
        countDownLatch.await();
        Assertions.assertTrue(callbackExecuted.get());
        Assertions.assertEquals(1, callBackExecuteTimes.get());
    }
}
