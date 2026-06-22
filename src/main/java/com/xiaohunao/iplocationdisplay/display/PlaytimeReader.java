package com.xiaohunao.iplocationdisplay.display;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;

public final class PlaytimeReader {
    private final int hourThreshold;

    public PlaytimeReader(int hourThreshold) {
        this.hourThreshold = hourThreshold;
    }

    public String getPlaytime(ServerPlayer player) {
        ServerStatsCounter stats = player.getStats();
        int ticks = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE));

        int hours = ticks / 72000;

        if (hours >= hourThreshold) {
            return hours + "h";
        } else {
            int minutes = ticks / 1200;
            return minutes + "m";
        }
    }
}
