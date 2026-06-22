package com.xiaohunao.iplocationdisplay.display;

import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RemoteAddressReaderTest {
    private final RemoteAddressReader reader = new RemoteAddressReader();

    @Test
    void readsAddressFromServerGamePacketListenerStyleGetter() {
        DirectConnectionAddressListener listener = new DirectConnectionAddressListener();

        assertEquals("/8.8.8.8:51234", reader.read(listener));
    }

    @Test
    void readsAddressFromNestedConnectionGetter() {
        NestedConnectionListener listener = new NestedConnectionListener();

        assertEquals("/1.1.1.1:25565", reader.read(listener));
    }

    @Test
    void returnsEmptyWhenAddressCannotBeRead() {
        assertEquals("", reader.read(new Object()));
    }

    public static final class DirectConnectionAddressListener {
        public InetSocketAddress getConnectionAddress() {
            return new InetSocketAddress("8.8.8.8", 51234);
        }
    }

    public static final class NestedConnectionListener {
        public Connection getConnection() {
            return new Connection();
        }
    }

    public static final class Connection {
        public InetSocketAddress getRemoteAddress() {
            return new InetSocketAddress("1.1.1.1", 25565);
        }
    }
}
