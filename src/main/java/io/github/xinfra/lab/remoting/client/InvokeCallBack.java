package io.github.xinfra.lab.remoting.client;

import io.github.xinfra.lab.remoting.message.Message;

public interface InvokeCallBack {
    void complete(Message result);
}
