package com.xiaohunao.iplocationdisplay.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpLocationConfigTest {
    @Test
    void usesServerWideConfigFileInRootConfigDirectory() {
        assertEquals("COMMON", IpLocationConfig.CONFIG_TYPE_NAME);
        assertEquals("iplocationdisplay.toml", IpLocationConfig.CONFIG_FILE_NAME);
    }

    @Test
    void testShowPlaytimeDefault() {
        assertTrue(IpLocationConfig.SHOW_PLAYTIME.get());
    }

    @Test
    void testPlaytimeHourThresholdDefault() {
        assertEquals(1, IpLocationConfig.PLAYTIME_HOUR_THRESHOLD.get());
    }
}
