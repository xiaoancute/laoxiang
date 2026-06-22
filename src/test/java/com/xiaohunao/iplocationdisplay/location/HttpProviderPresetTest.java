package com.xiaohunao.iplocationdisplay.location;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpProviderPresetTest {
    @Test
    void ipSbPresetUsesAccurateChinaFriendlyDefaults() {
        HttpProviderPreset preset = HttpProviderPreset.resolve(
                "ip-sb",
                "https://example.invalid/%ip%",
                "status",
                "success",
                "%country% %regionName% %city%"
        );

        assertEquals("https://api.ip.sb/geoip/%ip%", preset.urlTemplate());
        assertEquals("", preset.successJsonPath());
        assertEquals("", preset.successValue());
        assertEquals("%country_localized% %region_localized% %city_localized%", preset.locationTemplate());
    }

    @Test
    void customPresetUsesConfiguredValues() {
        HttpProviderPreset preset = HttpProviderPreset.resolve(
                "custom",
                "https://example.invalid/%ip%",
                "ok",
                "true",
                "%data.location%"
        );

        assertEquals("https://example.invalid/%ip%", preset.urlTemplate());
        assertEquals("ok", preset.successJsonPath());
        assertEquals("true", preset.successValue());
        assertEquals("%data.location%", preset.locationTemplate());
    }
}
