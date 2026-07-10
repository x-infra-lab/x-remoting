package io.github.xinfra.lab.remoting.message;

/**
 * Marker interface for transport-layer messages.
 * <p>
 * Protocol-specific content (id, type, headers, body, serialization) is defined by
 * protocol implementations, not by this interface.
 */
public interface Message {

}
