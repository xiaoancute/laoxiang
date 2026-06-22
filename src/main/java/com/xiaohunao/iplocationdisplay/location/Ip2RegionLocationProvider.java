package com.xiaohunao.iplocationdisplay.location;

import org.lionsoul.ip2region.xdb.Searcher;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public final class Ip2RegionLocationProvider implements LocationProvider {
    private final Path databasePath;

    public Ip2RegionLocationProvider(Path databasePath) {
        this.databasePath = databasePath;
    }

    @Override
    public Optional<IpLocation> lookup(String ip) {
        if (databasePath == null || !Files.isRegularFile(databasePath)) {
            return Optional.empty();
        }

        try {
            Searcher searcher = Searcher.newWithFileOnly(databasePath.toString());
            try {
                String region = searcher.search(ip);
                String location = formatRegion(region);
                return location.isEmpty() ? Optional.empty() : Optional.of(new IpLocation(location));
            } finally {
                searcher.close();
            }
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String formatRegion(String region) {
        if (region == null || region.isBlank()) {
            return "";
        }
        return Arrays.stream(region.split("\\|"))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .filter(part -> !"0".equals(part))
                .distinct()
                .collect(Collectors.joining(" "));
    }
}
