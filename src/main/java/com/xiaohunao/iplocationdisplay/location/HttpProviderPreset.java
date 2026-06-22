package com.xiaohunao.iplocationdisplay.location;

import java.util.Locale;

public record HttpProviderPreset(
        String urlTemplate,
        String successJsonPath,
        String successValue,
        String locationTemplate
) {
    public static HttpProviderPreset resolve(
            String presetName,
            String customUrlTemplate,
            String customSuccessJsonPath,
            String customSuccessValue,
            String customLocationTemplate
    ) {
        String normalized = presetName == null ? "ip-sb" : presetName.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "custom" -> new HttpProviderPreset(
                    customUrlTemplate,
                    customSuccessJsonPath,
                    customSuccessValue,
                    customLocationTemplate
            );
            case "ip-api-com" -> new HttpProviderPreset(
                    "http://ip-api.com/json/%ip%?lang=zh-CN&fields=status,message,country,regionName,city,query,isp,org,as,asname",
                    "status",
                    "success",
                    "%country% %regionName% %city% %isp_localized%"
            );
            default -> new HttpProviderPreset(
                    "https://api.ip.sb/geoip/%ip%",
                    "",
                    "",
                    "%country_localized% %region_localized% %city_localized% %isp_localized%"
            );
        };
    }
}
