package io.github.xinfra.lab.remoting.client;


import io.github.xinfra.lab.remoting.annotation.OnlyForTest;
import io.github.xinfra.lab.remoting.connection.Connection;
import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.util.Timeout;
import lombok.Getter;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class InvokeFuture {

    @Getter
    private int requestId;

    @Getter
    private Connection connection;

    private final CountDownLatch countDownLatch;

    private Message message;

    @OnlyForTest
    @Getter
    private Timeout timeout;

    private InvokeCallBack invokeCallBack;

    private final AtomicBoolean callBackExecuted = new AtomicBoolean(false);

    private ClassLoader classLoader;

    public InvokeFuture(int requestId, Connection connection) {
        this.requestId = requestId;
        this.connection = connection;
        this.countDownLatch = new CountDownLatch(1);
        this.classLoader = Thread.currentThread().getContextClassLoader();
    }

    public void addTimeout(Timeout timeout) {
        Validate.isTrue(this.timeout == null, "repeat add timeout for InvokeFuture");
        this.timeout = timeout;
    }

    public void addCallBack(InvokeCallBack invokeCallBack) {
        Validate.isTrue(this.invokeCallBack == null, "repeat add invokeCallBack for InvokeFuture");
        this.invokeCallBack = invokeCallBack;
    }

    public void asyncExecuteCallBack() {
        try {
            ProtocolType protocolType = message.protocolType();
            Protocol protocol = ProtocolManager.getProtocol(protocolType);
            Executor executor = protocol.messageHandler().executor();

            executor.execute(() -> {
                try {
                    executeCallBack();
                } catch (Throwable t) {
                    log.error("executeCallBack fail. id:{}", message.id(), t);
                }
            });

        } catch (Exception e) {
            log.error("asyncExecuteCallBack fail. id:{}", message.id(), e);
        }
    }

    public void executeCallBack() {
        if (invokeCallBack != null) {
            if (isDone()) {
                if (callBackExecuted.compareAndSet(false, true)) {
                    ClassLoader contextClassLoader = null;
                    try {
                        ClassLoader appClassLoader = getAppClassLoader();
                        if (appClassLoader != null) {
                            contextClassLoader = Thread.currentThread().getContextClassLoader();
                            Thread.currentThread().setContextClassLoader(appClassLoader);
                        }
                        invokeCallBack.complete(this);
                    } finally {
                        if (contextClassLoader != null) {
                            Thread.currentThread().setContextClassLoader(contextClassLoader);
                        }
                    }
                }
            }
        }
    }

    public ClassLoader getAppClassLoader() {
        return classLoader;
    }

    public void finish(Message result) {
        Validate.isTrue(this.message == null, "requestId: %s InvokeFuture already finished.", requestId);
        this.message = result;
        countDownLatch.countDown();
    }

    public boolean isDone() {
        return countDownLatch.getCount() <= 0;
    }

    public Message await() throws InterruptedException {
        countDownLatch.await();
        return message;
    }

    /**
     * @param timeout
     * @param unit
     * @return null if timeout
     * @throws InterruptedException
     */
    public Message await(long timeout, TimeUnit unit) throws InterruptedException {
        boolean finished = countDownLatch.await(timeout, unit);
        if (!finished) {
            return null;
        }
        return message;
    }

    public boolean cancelTimeout() {
        if (timeout != null) {
            return timeout.cancel();
        }
        return false;
    }

    public Message createConnectionClosedMessage() {
        ProtocolType protocolType = connection.getEndpoint().getProtocolType();
        Protocol protocol = ProtocolManager.getProtocol(protocolType);
        return protocol.messageFactory().createConnectionClosedMessage(requestId);
    }
}
