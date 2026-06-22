package com.xiaohunao.iplocationdisplay.config;

import com.xiaohunao.iplocationdisplay.location.ProviderMode;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Path;

public final class IpLocationConfig {
    public static final String CONFIG_TYPE_NAME = "COMMON";
    public static final String CONFIG_FILE_NAME = "iplocationdisplay.toml";

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLED = BUILDER
            .comment("Enable IP location displays.")
            .define("enabled", true);
    public static final ModConfigSpec.ConfigValue<String> PROVIDER_MODE = BUILDER
            .comment("Location provider mode: local, http, or hybrid.")
            .define("providerMode", "http");
    public static final ModConfigSpec.ConfigValue<String> HTTP_PRESET = BUILDER
            .comment("HTTP provider preset: ip-sb, ip-api-com, or custom.")
            .define("httpPreset", "ip-sb");
    public static final ModConfigSpec.ConfigValue<String> DISPLAY_FORMAT = BUILDER
            .comment("Text format for the head display. Use %location% for the resolved location.")
            .define("displayFormat", "[%location%]");
    public static final ModConfigSpec.DoubleValue VERTICAL_OFFSET = BUILDER
            .comment("Vertical display offset above the player.")
            .defineInRange("verticalOffset", 2.6D, 0.0D, 10.0D);
    public static final ModConfigSpec.IntValue TICK_INTERVAL = BUILDER
            .comment("Display position update interval in server ticks.")
            .defineInRange("tickInterval", 1, 1, 200);
    public static final ModConfigSpec.BooleanValue SHOW_UNKNOWN = BUILDER
            .comment("Show unknownText when a public IP cannot be resolved.")
            .define("showUnknown", false);
    public static final ModConfigSpec.ConfigValue<String> UNKNOWN_TEXT = BUILDER
            .comment("Fallback text when lookup fails and showUnknown is enabled.")
            .define("unknownText", "Unknown");
    public static final ModConfigSpec.ConfigValue<String> LOCAL_TEXT = BUILDER
            .comment("Text used for local, private, and reserved addresses.")
            .define("localText", "Local");
    public static final ModConfigSpec.ConfigValue<String> DATABASE_PATH = BUILDER
            .comment("Path to the optional ip2region.xdb database.")
            .define("databasePath", "config/iplocationdisplay/ip2region.xdb");
    public static final ModConfigSpec.ConfigValue<String> HTTP_URL_TEMPLATE = BUILDER
            .comment("HTTP lookup URL template. Use %ip% for the player IP.")
            .define("httpUrlTemplate", "https://api.ip.sb/geoip/%ip%");
    public static final ModConfigSpec.ConfigValue<String> HTTP_SUCCESS_JSON_PATH = BUILDER
            .comment("JSON path containing the provider success marker.")
            .define("httpSuccessJsonPath", "");
    public static final ModConfigSpec.ConfigValue<String> HTTP_SUCCESS_VALUE = BUILDER
            .comment("Expected success marker value.")
            .define("httpSuccessValue", "");
    public static final ModConfigSpec.ConfigValue<String> HTTP_LOCATION_TEMPLATE = BUILDER
            .comment("Location template built from HTTP JSON fields.")
            .define("httpLocationTemplate", "%country_localized% %region_localized% %city_localized%");
    public static final ModConfigSpec.IntValue HTTP_TIMEOUT_MILLIS = BUILDER
            .comment("HTTP lookup timeout in milliseconds.")
            .defineInRange("httpTimeoutMillis", 2000, 100, 30000);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private IpLocationConfig() {
    }

    public static RuntimeSettings runtimeSettings() {
        return new RuntimeSettings(
                ENABLED.get(),
                ProviderMode.fromConfig(PROVIDER_MODE.get()),
                HTTP_PRESET.get(),
                DISPLAY_FORMAT.get(),
                VERTICAL_OFFSET.get(),
                TICK_INTERVAL.get(),
                SHOW_UNKNOWN.get(),
                UNKNOWN_TEXT.get(),
                LOCAL_TEXT.get(),
                Path.of(DATABASE_PATH.get()),
                HTTP_URL_TEMPLATE.get(),
                HTTP_SUCCESS_JSON_PATH.get(),
                HTTP_SUCCESS_VALUE.get(),
                HTTP_LOCATION_TEMPLATE.get(),
                HTTP_TIMEOUT_MILLIS.get()
        );
    }

    public record RuntimeSettings(
            boolean enabled,
            ProviderMode providerMode,
            String httpPreset,
            String displayFormat,
            double verticalOffset,
            int tickInterval,
            boolean showUnknown,
            String unknownText,
            String localText,
            Path databasePath,
            String httpUrlTemplate,
            String httpSuccessJsonPath,
            String httpSuccessValue,
            String httpLocationTemplate,
            int httpTimeoutMillis
    ) {
    }
}
