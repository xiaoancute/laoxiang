package com.xiaohunao.iplocationdisplay.location;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocationProvidersTest {
    @Test
    void httpModeUsesOnlyHttpProvider() throws Exception {
        CountingProvider local = new CountingProvider(Optional.of(new IpLocation("Local")));
        CountingProvider http = new CountingProvider(Optional.of(new IpLocation("Http")));

        Optional<IpLocation> result = LocationProviders.choose(ProviderMode.HTTP, local, http).lookup("8.8.8.8");

        assertEquals(Optional.of(new IpLocation("Http")), result);
        assertEquals(0, local.calls);
        assertEquals(1, http.calls);
    }

    @Test
    void localModeUsesOnlyLocalProvider() throws Exception {
        CountingProvider local = new CountingProvider(Optional.of(new IpLocation("Local")));
        CountingProvider http = new CountingProvider(Optional.of(new IpLocation("Http")));

        Optional<IpLocation> result = LocationProviders.choose(ProviderMode.LOCAL, local, http).lookup("8.8.8.8");

        assertEquals(Optional.of(new IpLocation("Local")), result);
        assertEquals(1, local.calls);
        assertEquals(0, http.calls);
    }

    @Test
    void hybridModeReturnsLocalResultFirst() throws Exception {
        CountingProvider local = new CountingProvider(Optional.of(new IpLocation("Local")));
        CountingProvider http = new CountingProvider(Optional.of(new IpLocation("Http")));

        Optional<IpLocation> result = LocationProviders.choose(ProviderMode.HYBRID, local, http).lookup("8.8.8.8");

        assertEquals(Optional.of(new IpLocation("Local")), result);
        assertEquals(1, local.calls);
        assertEquals(0, http.calls);
    }

    @Test
    void hybridModeFallsBackToHttpWhenLocalMisses() throws Exception {
        CountingProvider local = new CountingProvider(Optional.empty());
        CountingProvider http = new CountingProvider(Optional.of(new IpLocation("Http")));

        Optional<IpLocation> result = LocationProviders.choose(ProviderMode.HYBRID, local, http).lookup("8.8.8.8");

        assertEquals(Optional.of(new IpLocation("Http")), result);
        assertEquals(1, local.calls);
        assertEquals(1, http.calls);
    }

    private static final class CountingProvider implements LocationProvider {
        private final Optional<IpLocation> result;
        private int calls;

        private CountingProvider(Optional<IpLocation> result) {
            this.result = result;
        }

        @Override
        public Optional<IpLocation> lookup(String ip) {
            calls++;
            return result;
        }
    }
}
