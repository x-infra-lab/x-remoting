package io.github.xinfra.lab.remoting.protocol;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ProtocolManager {
    private static ConcurrentHashMap<ProtocolType, Protocol> protocols = new ConcurrentHashMap<>();


    public static Set<ProtocolType> getProtocolTypes() {
        return protocols.keySet();
    }

    public static Protocol getProtocol(ProtocolType protocolType) {
        return protocols.get(protocolType);
    }


    public static void registerProtocolIfAbsent(ProtocolType protocolType,
                                                Protocol protocol) {
        protocols.putIfAbsent(protocolType, protocol);
    }
}
