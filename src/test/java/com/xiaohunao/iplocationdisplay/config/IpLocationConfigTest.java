package com.xiaohunao.iplocationdisplay.config;

import net.neoforged.fml.config.ModConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IpLocationConfigTest {
    @Test
    void usesServerWideConfigFileInRootConfigDirectory() {
        assertEquals(ModConfig.Type.COMMON, IpLocationConfig.CONFIG_TYPE);
        assertEquals("iplocationdisplay.toml", IpLocationConfig.CONFIG_FILE_NAME);
    }
}
