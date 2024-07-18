package io.github.xinfra.lab.remoting.protocol;

import org.junit.Assert;
import org.junit.Test;

public class ProtocolManagerTest {

    @Test
    public void test() {
        ProtocolType test = new ProtocolType("ProtocolManagerTest", "ProtocolManagerTest".getBytes());
        TestProtocol testProtocol1 = new TestProtocol();
        ProtocolManager.registerProtocolIfAbsent(test, testProtocol1);

        TestProtocol testProtocol2 = new TestProtocol();
        ProtocolManager.registerProtocolIfAbsent(test, testProtocol2);

        Assert.assertTrue(ProtocolManager.getProtocol(test) == testProtocol1);
    }
}
