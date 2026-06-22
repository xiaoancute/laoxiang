package com.xiaohunao.iplocationdisplay.location;

import java.util.Optional;

public final class LocationProviders {
    private LocationProviders() {
    }

    public static LocationProvider choose(ProviderMode mode, LocationProvider local, LocationProvider http) {
        return switch (mode == null ? ProviderMode.HTTP : mode) {
            case LOCAL -> local;
            case HTTP -> http;
            case HYBRID -> ip -> {
                Optional<IpLocation> localResult = local.lookup(ip);
                return localResult.isPresent() ? localResult : http.lookup(ip);
            };
        };
    }
}
