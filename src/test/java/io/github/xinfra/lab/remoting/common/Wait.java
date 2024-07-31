package io.github.xinfra.lab.remoting.common;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public class Wait {

    /**
     * @param condition
     * @param sleepMills
     * @param attemptNum
     */
    public static void untilIsTrue(Supplier<Boolean> condition, int sleepMills, int attemptNum) throws InterruptedException, TimeoutException {
        for (int i = 0; i < attemptNum; i++) {
            if (condition.get()) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(sleepMills);
        }
        throw new TimeoutException("no result get until the end. ");
    }


    /**
     * @param condition
     * @param sleepMills
     * @param attemptNum
     */
    public static <T> T untilIsPresent(Supplier<Optional<T>> condition, int sleepMills, int attemptNum) throws InterruptedException, TimeoutException {
        for (int i = 0; i < attemptNum; i++) {
            if (condition.get().isPresent()) {
                return condition.get().get();
            }
            TimeUnit.MILLISECONDS.sleep(sleepMills);
        }
        throw new TimeoutException("no result get until the end. ");
    }
}
