package io.github.xinfra.lab.remoting.connection;

import java.util.List;

public interface ConnectionSelectStrategy {

    Connection select(List<Connection> connections);
}