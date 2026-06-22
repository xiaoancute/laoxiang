package com.xiaohunao.iplocationdisplay.location;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressNormalizerTest {
    private final AddressNormalizer normalizer = new AddressNormalizer();

    @Test
    void stripsSlashAndPortFromIpv4SocketAddress() {
        assertEquals(Optional.of("1.2.3.4"), normalizer.normalize("/1.2.3.4:51234"));
    }

    @Test
    void stripsPortFromPlainIpv4Address() {
        assertEquals(Optional.of("8.8.8.8"), normalizer.normalize("8.8.8.8:25565"));
    }

    @Test
    void stripsBracketsAndPortFromIpv6SocketAddress() {
        assertEquals(Optional.of("2001:4860:4860::8888"), normalizer.normalize("/[2001:4860:4860::8888]:51234"));
    }

    @Test
    void keepsPlainIpv6Address() {
        assertEquals(Optional.of("2001:4860:4860::8888"), normalizer.normalize("2001:4860:4860::8888"));
    }

    @Test
    void rejectsBlankAndInvalidAddresses() {
        assertEquals(Optional.empty(), normalizer.normalize(""));
        assertEquals(Optional.empty(), normalizer.normalize("not an ip"));
    }

    @Test
    void detectsLocalAndPrivateIpv4Addresses() {
        assertTrue(normalizer.isLocalOrPrivate("127.0.0.1"));
        assertTrue(normalizer.isLocalOrPrivate("10.0.0.1"));
        assertTrue(normalizer.isLocalOrPrivate("172.16.0.1"));
        assertTrue(normalizer.isLocalOrPrivate("192.168.1.1"));
        assertTrue(normalizer.isLocalOrPrivate("169.254.10.20"));
    }

    @Test
    void detectsLocalIpv6Addresses() {
        assertTrue(normalizer.isLocalOrPrivate("::1"));
        assertTrue(normalizer.isLocalOrPrivate("fe80::1"));
        assertTrue(normalizer.isLocalOrPrivate("fc00::1"));
    }

    @Test
    void treatsPublicAddressesAsPublic() {
        assertFalse(normalizer.isLocalOrPrivate("8.8.8.8"));
        assertFalse(normalizer.isLocalOrPrivate("2001:4860:4860::8888"));
    }
}
