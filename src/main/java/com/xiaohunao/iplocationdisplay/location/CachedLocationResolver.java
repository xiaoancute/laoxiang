package com.xiaohunao.iplocationdisplay.location;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

public final class CachedLocationResolver {
    private final AddressNormalizer addressNormalizer;
    private final LocationProvider locationProvider;
    private final Executor executor;
    private final String localText;
    private final String unknownText;
    private final boolean showUnknown;
    private final ConcurrentMap<String, CompletableFuture<Optional<IpLocation>>> cache = new ConcurrentHashMap<>();

    public CachedLocationResolver(
            AddressNormalizer addressNormalizer,
            LocationProvider locationProvider,
            Executor executor,
            String localText,
            String unknownText,
            boolean showUnknown
    ) {
        this.addressNormalizer = addressNormalizer;
        this.locationProvider = locationProvider;
        this.executor = executor;
        this.localText = localText;
        this.unknownText = unknownText;
        this.showUnknown = showUnknown;
    }

    public CompletableFuture<Optional<IpLocation>> resolve(String rawAddress) {
        Optional<String> normalizedIp = addressNormalizer.normalize(rawAddress);
        if (normalizedIp.isEmpty()) {
            return CompletableFuture.completedFuture(unknown());
        }

        String ip = normalizedIp.get();
        if (addressNormalizer.isLocalOrPrivate(ip)) {
            return CompletableFuture.completedFuture(Optional.of(new IpLocation(localText)));
        }

        return cache.computeIfAbsent(ip, this::resolvePublicIp);
    }

    public void shutdown() {
        if (executor instanceof ExecutorService executorService) {
            executorService.shutdown();
        }
    }

    private CompletableFuture<Optional<IpLocation>> resolvePublicIp(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Optional<IpLocation> location = locationProvider.lookup(ip);
                return location.isPresent() ? location : unknown();
            } catch (Exception ignored) {
                return unknown();
            }
        }, executor);
    }

    private Optional<IpLocation> unknown() {
        return showUnknown ? Optional.of(new IpLocation(unknownText)) : Optional.empty();
    }
}
