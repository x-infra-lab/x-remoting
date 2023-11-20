package io.github.xinfra.lab.remoting.message;

import java.util.concurrent.Executor;

public interface MessageHandler {

    Executor executor();
}
