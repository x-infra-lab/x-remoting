package io.github.xinfra.lab.remoting.rpc.protocol;

import io.github.xinfra.lab.remoting.protocol.Protocol;
import io.github.xinfra.lab.remoting.rpc.message.MessageFactory;

public interface RpcProtocol extends Protocol {

	MessageFactory getMessageFactory();

}
