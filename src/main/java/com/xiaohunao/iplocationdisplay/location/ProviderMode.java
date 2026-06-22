package com.xiaohunao.iplocationdisplay.location;

import java.util.Locale;

public enum ProviderMode {
    LOCAL,
    HTTP,
    HYBRID;

    public static ProviderMode fromConfig(String value) {
        if (value == null || value.isBlank()) {
            return HTTP;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "local" -> LOCAL;
            case "hybrid" -> HYBRID;
            default -> HTTP;
        };
    }
}
