package io.github.xinfra.lab.remoting.rpc;

import io.github.xinfra.lab.remoting.processor.UserProcessor;

public class SimpleUserProcessor implements UserProcessor<SimpleRequest> {
    @Override
    public String interest() {
        return SimpleRequest.class.getName();
    }

    @Override
    public Object handRequest(SimpleRequest request) {
        return "echo:" + request.getMsg();
    }
}
