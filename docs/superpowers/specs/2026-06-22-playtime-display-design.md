# Playtime Display Feature Design

Date: 2026-06-22

## Goal

Add playtime tracking to the LaoXiang mod's head display. Show each player's total playtime alongside their IP location in the format: `中国 广东 广州 120h` or `中国 广东 广州 45m`.

## Requirements

- Read playtime from Minecraft's built-in statistics system (`world/stats/<uuid>.json`)
- Display hours for playtime ≥ 1 hour (e.g., `120h`)
- Display minutes for playtime < 1 hour (e.g., `45m`)
- Fully configurable via mod config
- Support template-based formatting with `%playtime%` placeholder
- Works for both existing players (historical data) and new players

## Non-Goals

- Custom playtime tracking or storage (use Minecraft's native stats only)
- Day-based display units
- Playtime limits or restrictions
- Per-player playtime reset

## Architecture

### New Components

**1. PlaytimeReader**
- Reads `Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE)` from `ServerPlayer.getStats()`
- Converts ticks to hours or minutes
- Returns formatted string (e.g., "120h" or "45m")

**2. Extended DisplayTextFormatter**
- Add `%playtime%` placeholder support
- Update `format()` method signature to accept playtime parameter
- Maintain backward compatibility with existing `%location%` placeholder

**3. Config Extensions**
- `showPlaytime` (boolean, default `true`) - enable/disable playtime display
- `playtimeHourThreshold` (int, default `1`) - show minutes when playtime < this value (in hours)
- Update `displayFormat` default to include playtime

### Data Flow

1. Player login → `PlayerDisplayManager.onPlayerLogin()`
2. Resolve IP location (existing flow)
3. **NEW**: Read playtime via `PlaytimeReader.getPlaytime(player)`
4. **NEW**: Format display text with both location and playtime
5. Create/update TextDisplay entity (existing flow)

## Implementation Details

### PlaytimeReader Class

```java
public final class PlaytimeReader {
    private final int hourThreshold;
    
    public PlaytimeReader(int hourThreshold) {
        this.hourThreshold = hourThreshold;
    }
    
    public String getPlaytime(ServerPlayer player) {
        ServerStatsCounter stats = player.getStats();
        int ticks = stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE));
        
        int hours = ticks / 72000; // 20 ticks/sec * 60 sec * 60 min
        
        if (hours >= hourThreshold) {
            return hours + "h";
        } else {
            int minutes = ticks / 1200; // 20 ticks/sec * 60 sec
            return minutes + "m";
        }
    }
}
```

**Edge Cases:**
- If stats file doesn't exist or is corrupted, `getValue()` returns 0
- New players will show "0m" initially
- Negative values are not possible (stats are cumulative)

### DisplayTextFormatter Changes

**Old signature:**
```java
public String format(String displayFormat, String location)
```

**New signature:**
```java
public String format(String displayFormat, String location, String playtime)
```

**Logic:**
1. Replace `%location%` with location value
2. Replace `%playtime%` with playtime value
3. Trim and return result

**Backward compatibility:** If `playtime` is null or empty, and `%playtime%` appears in the template, replace it with empty string.

### Config Changes

Add to `IpLocationConfig.java`:

```java
public static final ModConfigSpec.BooleanValue SHOW_PLAYTIME = BUILDER
    .comment("Show player playtime in the head display.")
    .define("showPlaytime", true);

public static final ModConfigSpec.IntValue PLAYTIME_HOUR_THRESHOLD = BUILDER
    .comment("Show minutes when playtime is below this hour threshold.")
    .defineInRange("playtimeHourThreshold", 1, 0, 100);
```

Update default display format:
```java
.define("displayFormat", "%location% %playtime%");
```

Add to `RuntimeSettings` record:
```java
public record RuntimeSettings(
    // ... existing fields
    boolean showPlaytime,
    int playtimeHourThreshold
) {}
```

### PlayerDisplayManager Changes

**Constructor:**
Add `PlaytimeReader` dependency:
```java
private final PlaytimeReader playtimeReader;

public PlayerDisplayManager(
    CachedLocationResolver resolver,
    DisplayTextFormatter displayTextFormatter,
    IpLocationConfig.RuntimeSettings settings
) {
    // ... existing code
    this.playtimeReader = new PlaytimeReader(settings.playtimeHourThreshold());
}
```

**Display creation:**
Update `createOrReplace()` method:
```java
private void createOrReplace(ServerPlayer player, IpLocation ipLocation) {
    // ... existing code
    
    String playtimeStr = settings.showPlaytime() 
        ? playtimeReader.getPlaytime(player)
        : "";
    
    String text = displayTextFormatter.format(
        settings.displayFormat(), 
        ipLocation.value(),
        playtimeStr
    );
    
    // ... rest of display creation
}
```

## Configuration Examples

### Show location and playtime (default):
```toml
displayFormat = "%location% %playtime%"
showPlaytime = true
playtimeHourThreshold = 1
```
Result: `中国 广东 广州 120h`

### Show only location:
```toml
displayFormat = "%location%"
showPlaytime = false
```
Result: `中国 广东 广州`

### Custom format:
```toml
displayFormat = "[%location% | 游玩: %playtime%]"
showPlaytime = true
```
Result: `[中国 广东 广州 | 游玩: 120h]`

### Always show hours (never minutes):
```toml
playtimeHourThreshold = 0
```
Result: New players show `0h` instead of `45m`

## Error Handling

- **Stats file missing**: `getValue()` returns 0, displays `0m` or `0h`
- **Stats corrupted**: Minecraft handles corruption, returns default value (0)
- **Server restart**: Stats persist across restarts (stored in world files)
- **Player never joined before**: Fresh stats file created on first join, starts at 0

## Testing

### Unit Tests

**PlaytimeReaderTest:**
- Zero playtime → "0m"
- 30 minutes (36,000 ticks) → "30m"
- 59 minutes (70,800 ticks) → "59m"
- 1 hour (72,000 ticks) → "1h"
- 120 hours → "120h"
- Custom threshold: hourThreshold=2, 1.5 hours → "90m"

**DisplayTextFormatterTest:**
- Format with both placeholders: `"%location% %playtime%"` → `"广东 120h"`
- Format with only location: `"%location%"` → `"广东"`
- Empty playtime: `"%location% %playtime%"` with `playtime=""` → `"广东 "`
- No placeholders: `"[固定文本]"` → `"[固定文本]"`

### Integration Tests

**Manual verification:**
1. Build mod and run on test server
2. Join with new player → verify shows `0m`
3. Wait 5 real minutes → verify shows `5m`
4. Mock stats file with 1 hour → verify shows `1h`
5. Mock stats file with 2.5 hours → verify shows `2h` (not `150m`)
6. Change config `showPlaytime=false` → verify playtime disappears
7. Change config `displayFormat="%location%"` → verify only location shows

## Performance Considerations

**Stats reading cost:**
- `ServerPlayer.getStats()` returns cached `ServerStatsCounter` instance
- `getValue()` is a simple map lookup
- Per-player overhead: ~1 map lookup per login + ~1 map lookup per tick interval
- Negligible impact even with 100+ players

**No additional I/O:**
- Stats are already loaded in memory by Minecraft
- No file reads or database queries needed
- No network calls

## Backward Compatibility

- Existing configs without `showPlaytime` will use default (`true`)
- Existing `displayFormat` values without `%playtime%` will continue to work
- Old display format `[%location%]` remains valid
- Migration: On first load with new version, playtime will appear for servers using default format

## Future Extensions

**Possible future features (not in this design):**
- Playtime brackets (萌新/老玩家/大佬) based on thresholds
- Day-based units for very long playtimes
- Session playtime vs. total playtime
- Playtime leaderboard commands

These are explicitly out of scope for this initial implementation.
