package io.github.xinfra.lab.remoting.protocol;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ProtocolManager {
    private static ConcurrentHashMap<ProtocolType, Protocol> protocols = new ConcurrentHashMap<>();

    public static Protocol getProtocol(ProtocolType protocolType) {
        return protocols.get(protocolType);
    }


    public static void registerProtocolIfAbsent(ProtocolType protocolType,
                                                Protocol protocol) {
        Protocol preProtocol = protocols.putIfAbsent(protocolType, protocol);
        if (preProtocol != null) {
            log.warn("repeat registerProtocol protocolType:{}", protocolType);
        }
    }
}
