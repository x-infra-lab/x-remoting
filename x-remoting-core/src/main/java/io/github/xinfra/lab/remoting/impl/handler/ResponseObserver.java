package io.github.xinfra.lab.remoting.impl.handler;

import io.github.xinfra.lab.remoting.message.MessageExchange;

public class ResponseObserver<R> {
    private final MessageExchange messageExchange;

    public ResponseObserver(MessageExchange messageExchange) {
        this.messageExchange = messageExchange;
    }

    public void complete(R result) {


        // todo @joecqupt
	}

}
