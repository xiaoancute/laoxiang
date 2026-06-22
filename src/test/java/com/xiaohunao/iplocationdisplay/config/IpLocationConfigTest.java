package com.xiaohunao.iplocationdisplay.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpLocationConfigTest {
    @Test
    void usesServerWideConfigFileInRootConfigDirectory() {
        assertEquals("COMMON", IpLocationConfig.CONFIG_TYPE_NAME);
        assertEquals("iplocationdisplay.toml", IpLocationConfig.CONFIG_FILE_NAME);
    }
}
