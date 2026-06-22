package com.xiaohunao.iplocationdisplay.location;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Ip2RegionLocationProviderTest {
    @Test
    void returnsEmptyWhenDatabaseFileIsMissing() throws Exception {
        Ip2RegionLocationProvider provider = new Ip2RegionLocationProvider(Path.of("missing/ip2region.xdb"));

        assertEquals(Optional.empty(), provider.lookup("8.8.8.8"));
    }
}
