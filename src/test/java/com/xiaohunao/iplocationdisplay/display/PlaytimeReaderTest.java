package com.xiaohunao.iplocationdisplay.display;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public final class PlaytimeReaderTest {
    @Test
    public void testZeroPlaytime() {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerStatsCounter stats = mock(ServerStatsCounter.class);
        when(player.getStats()).thenReturn(stats);
        when(stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE))).thenReturn(0);

        PlaytimeReader reader = new PlaytimeReader(1);
        assertEquals("0m", reader.getPlaytime(player));
    }

    @Test
    public void testThirtyMinutes() {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerStatsCounter stats = mock(ServerStatsCounter.class);
        when(player.getStats()).thenReturn(stats);
        when(stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE))).thenReturn(36000);

        PlaytimeReader reader = new PlaytimeReader(1);
        assertEquals("30m", reader.getPlaytime(player));
    }

    @Test
    public void testFiftyNineMinutes() {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerStatsCounter stats = mock(ServerStatsCounter.class);
        when(player.getStats()).thenReturn(stats);
        when(stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE))).thenReturn(70800);

        PlaytimeReader reader = new PlaytimeReader(1);
        assertEquals("59m", reader.getPlaytime(player));
    }

    @Test
    public void testOneHour() {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerStatsCounter stats = mock(ServerStatsCounter.class);
        when(player.getStats()).thenReturn(stats);
        when(stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE))).thenReturn(72000);

        PlaytimeReader reader = new PlaytimeReader(1);
        assertEquals("1h", reader.getPlaytime(player));
    }

    @Test
    public void testOneHundredTwentyHours() {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerStatsCounter stats = mock(ServerStatsCounter.class);
        when(player.getStats()).thenReturn(stats);
        when(stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE))).thenReturn(8640000);

        PlaytimeReader reader = new PlaytimeReader(1);
        assertEquals("120h", reader.getPlaytime(player));
    }

    @Test
    public void testCustomThresholdShowsMinutes() {
        ServerPlayer player = mock(ServerPlayer.class);
        ServerStatsCounter stats = mock(ServerStatsCounter.class);
        when(player.getStats()).thenReturn(stats);
        when(stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE))).thenReturn(108000); // 1.5 hours

        PlaytimeReader reader = new PlaytimeReader(2);
        assertEquals("90m", reader.getPlaytime(player));
    }
}
