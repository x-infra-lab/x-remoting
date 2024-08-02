package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.RemotingContext;
import io.github.xinfra.lab.remoting.common.IDGenerator;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.message.MessageHandler;
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
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class InvokeFutureTest {
    private InvokeFuture invokeFuture;
    TestProtocol testProtocol;

    @BeforeEach
    public void before() {

        testProtocol = new TestProtocol();

        ExecutorService executor = Executors.newCachedThreadPool();

        // set message handler
        testProtocol.setTestMessageHandler(new MessageHandler() {
            @Override
            public Executor executor() {
                return executor;
            }

            @Override
            public void handleMessage(RemotingContext remotingContext, Object msg) {
                // do notiong
            }
        });


        final int requestId1 = IDGenerator.nextRequestId();
        invokeFuture = new InvokeFuture(requestId1, testProtocol);
    }

    @Test
    public void testTimeout() {
        Assertions.assertNull(invokeFuture.timeout);
        Assertions.assertFalse(invokeFuture.cancelTimeout());

        HashedWheelTimer timer = new HashedWheelTimer();

        Timeout timeout = timer.newTimeout(t -> {
        }, 3, TimeUnit.SECONDS);
        invokeFuture.addTimeout(timeout);
        Assertions.assertEquals(invokeFuture.timeout, timeout);

        Assertions.assertTrue(invokeFuture.cancelTimeout());
        Assertions.assertFalse(invokeFuture.cancelTimeout());
        Assertions.assertTrue(invokeFuture.timeout.isCancelled());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            invokeFuture.addTimeout(timeout);
        });
    }

    @Test
    public void testGet() throws InterruptedException {
        Message message = mock(Message.class);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            invokeFuture.complete(message);
        });

        Assertions.assertThrows(TimeoutException.class, () -> {
            invokeFuture.get(1, TimeUnit.SECONDS);
        });
        Assertions.assertFalse(invokeFuture.isDone());

        Message result = invokeFuture.get();
        Assertions.assertSame(result, message);
        Assertions.assertTrue(invokeFuture.isDone());

        result = invokeFuture.get();
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

            InvokeFuture future = new InvokeFuture(IDGenerator.nextRequestId(), testProtocol);
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

        invokeFuture.complete(message);

        invokeFuture.executeCallBack();
        Assertions.assertTrue(callbackExecuted.get());
        Assertions.assertEquals(1, callBackExecuteTimes.get());

        invokeFuture.executeCallBack();
        Assertions.assertTrue(callbackExecuted.get());
        Assertions.assertEquals(1, callBackExecuteTimes.get());
    }

    @Test
    public void testCallBackAsync() throws InterruptedException {


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

        invokeFuture.complete(message);

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
