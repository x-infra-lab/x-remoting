package io.github.xinfra.lab.remoting.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ProtocolManager {
    private static Map<ProtocolType, Protocol> protocols = new HashMap<>();


    public static Set<ProtocolType> getProtocolTypes() {
        return protocols.keySet();
    }

    public static Protocol getProtocol(ProtocolType protocolType) {
        return protocols.get(protocolType);
    }


    public static void registerProtocol(ProtocolType protocolType,
                                        Protocol protocol) {
        protocols.put(protocolType, protocol);
    }
}