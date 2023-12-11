package io.github.xinfra.lab.remoting.client;


import io.github.xinfra.lab.remoting.message.Message;
import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.protocol.ProtocolManager;
import io.github.xinfra.lab.remoting.protocol.ProtocolType;
import io.netty.util.Timeout;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class InvokeFuture {

    @Getter
    private int requestId;

    private final CountDownLatch countDownLatch;

    private Message result;

    private Timeout timeout;

    private InvokeCallBack invokeCallBack;

    private AtomicBoolean callBackExecuted = new AtomicBoolean(false);

    public InvokeFuture(int requestId) {
        this.requestId = requestId;
        this.countDownLatch = new CountDownLatch(1);
    }

    public void addTimeout(Timeout timeout) {
        this.timeout = timeout;
    }

    public void addCallBack(InvokeCallBack invokeCallBack) {
        this.invokeCallBack = invokeCallBack;
    }

    public void executeCallBack() {
        if (invokeCallBack != null) {
            if (isDone()) {
                if (callBackExecuted.compareAndSet(false, true)) {
                    try {
                        // TODO  ClassLoader??
                        // FIXME ClassLoader??
                        ProtocolType protocolType = result.protocolType();
                        Protocol protocol = ProtocolManager.getProtocol(protocolType);
                        Executor executor = protocol.messageHandler().executor();
                        executor.execute(() -> {
                            invokeCallBack.complete(result);
                        });
                    } catch (Throwable t) {
                        log.error("execute callback fail. id:{}", result.id());
                    }
                }
            }
        }
    }

    public void finish(Message result) {
        this.result = result;
        countDownLatch.countDown();
    }

    public boolean isDone() {
        return countDownLatch.getCount() <= 0;
    }

    public Message await() throws InterruptedException {
        countDownLatch.await();
        return result;
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
        return result;
    }

    public boolean cancelTimeout() {
        if (timeout != null) {
            return timeout.cancel();
        }
        return false;
    }
}
