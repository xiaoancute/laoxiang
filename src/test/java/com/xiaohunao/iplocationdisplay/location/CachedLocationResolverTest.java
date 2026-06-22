package com.xiaohunao.iplocationdisplay.location;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CachedLocationResolverTest {
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @Test
    void localPrivateIpsReturnLocalTextWithoutProviderLookup() {
        CountingProvider provider = new CountingProvider(Optional.of(new IpLocation("Provider")));
        CachedLocationResolver resolver = resolver(provider, false);

        Optional<IpLocation> result = resolver.resolve("/127.0.0.1:51234").join();

        assertEquals(Optional.of(new IpLocation("Local")), result);
        assertEquals(0, provider.calls);
    }

    @Test
    void publicIpsUseProviderOnceAndCacheResult() {
        CountingProvider provider = new CountingProvider(Optional.of(new IpLocation("美国 弗吉尼亚州 Ashburn")));
        CachedLocationResolver resolver = resolver(provider, false);

        Optional<IpLocation> first = resolver.resolve("/8.8.8.8:51234").join();
        Optional<IpLocation> second = resolver.resolve("8.8.8.8:25565").join();

        assertEquals(Optional.of(new IpLocation("美国 弗吉尼亚州 Ashburn")), first);
        assertEquals(first, second);
        assertEquals(1, provider.calls);
    }

    @Test
    void providerFailuresAreCachedAsEmptyWhenUnknownIsHidden() {
        CountingProvider provider = new CountingProvider(Optional.empty());
        CachedLocationResolver resolver = resolver(provider, false);

        Optional<IpLocation> first = resolver.resolve("8.8.8.8").join();
        Optional<IpLocation> second = resolver.resolve("8.8.8.8").join();

        assertEquals(Optional.empty(), first);
        assertEquals(Optional.empty(), second);
        assertEquals(1, provider.calls);
    }

    @Test
    void unknownTextIsReturnedWhenEnabled() {
        CountingProvider provider = new CountingProvider(Optional.empty());
        CachedLocationResolver resolver = resolver(provider, true);

        assertEquals(Optional.of(new IpLocation("Unknown")), resolver.resolve("8.8.8.8").join());
    }

    @Test
    void invalidAddressUsesUnknownTextOnlyWhenEnabled() {
        CountingProvider provider = new CountingProvider(Optional.of(new IpLocation("Provider")));

        assertEquals(Optional.empty(), resolver(provider, false).resolve("not an ip").join());
        assertEquals(Optional.of(new IpLocation("Unknown")), resolver(provider, true).resolve("not an ip").join());
    }

    private CachedLocationResolver resolver(LocationProvider provider, boolean showUnknown) {
        return new CachedLocationResolver(
                new AddressNormalizer(),
                provider,
                DIRECT_EXECUTOR,
                "Local",
                "Unknown",
                showUnknown
        );
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
