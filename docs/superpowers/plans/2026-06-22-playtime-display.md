# Playtime Display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add playtime display to LaoXiang mod's head display, showing format like `中国 广东 广州 120h` or `中国 广东 广州 45m`.

**Architecture:** Pure Java `PlaytimeReader` reads Minecraft's native stats system and formats time strings. Extended `DisplayTextFormatter` supports `%playtime%` placeholder. Config controls display and threshold. No custom storage or tracking needed.

**Tech Stack:** Java 21, NeoForge 1.21.1, JUnit 5, Minecraft Stats API

## Global Constraints

- Java 21
- NeoForge 1.21.1
- Use Minecraft's native stats system only (`Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE)`)
- No custom playtime storage or tracking
- Maintain backward compatibility with existing configs

---

## File Structure

- **Create:** `src/main/java/com/xiaohunao/iplocationdisplay/display/PlaytimeReader.java` - reads player stats and formats time strings
- **Modify:** `src/main/java/com/xiaohunao/iplocationdisplay/display/DisplayTextFormatter.java` - add `%playtime%` placeholder support
- **Modify:** `src/main/java/com/xiaohunao/iplocationdisplay/config/IpLocationConfig.java` - add playtime config fields
- **Modify:** `src/main/java/com/xiaohunao/iplocationdisplay/display/PlayerDisplayManager.java` - wire playtime reading into display creation
- **Create:** `src/test/java/com/xiaohunao/iplocationdisplay/display/PlaytimeReaderTest.java` - test time conversion logic
- **Modify:** `src/test/java/com/xiaohunao/iplocationdisplay/display/DisplayTextFormatterTest.java` - test new placeholder

## Task 1: PlaytimeReader with Time Conversion

**Files:**
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/display/PlaytimeReader.java`
- Test: `src/test/java/com/xiaohunao/iplocationdisplay/display/PlaytimeReaderTest.java`

**Interfaces:**
- Consumes: Nothing (reads from Minecraft Stats API directly)
- Produces: `PlaytimeReader(int hourThreshold)` constructor, `String getPlaytime(ServerPlayer player)` method

- [ ] **Step 1: Write failing tests for zero playtime**

```java
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
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests '*PlaytimeReaderTest.testZeroPlaytime'`

Expected: Compilation error - `PlaytimeReader` class does not exist

- [ ] **Step 3: Create PlaytimeReader with minimal implementation**

```java
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
```

- [ ] **Step 4: Add Mockito dependency to build.gradle if not present**

Check if Mockito is already in dependencies. If not, add:

```groovy
testImplementation 'org.mockito:mockito-core:5.+'
```

- [ ] **Step 5: Run test to verify it passes**

Run: `gradle test --tests '*PlaytimeReaderTest.testZeroPlaytime'`

Expected: PASS

- [ ] **Step 6: Write tests for minutes display**

Add to `PlaytimeReaderTest.java`:

```java
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
```

- [ ] **Step 7: Run tests to verify they pass**

Run: `gradle test --tests '*PlaytimeReaderTest'`

Expected: All tests PASS

- [ ] **Step 8: Write tests for hours display**

Add to `PlaytimeReaderTest.java`:

```java
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
```

- [ ] **Step 9: Run tests to verify they pass**

Run: `gradle test --tests '*PlaytimeReaderTest'`

Expected: All tests PASS

- [ ] **Step 10: Write test for custom threshold**

Add to `PlaytimeReaderTest.java`:

```java
@Test
public void testCustomThresholdShowsMinutes() {
    ServerPlayer player = mock(ServerPlayer.class);
    ServerStatsCounter stats = mock(ServerStatsCounter.class);
    when(player.getStats()).thenReturn(stats);
    when(stats.getValue(Stats.CUSTOM.get(Stats.PLAY_ONE_MINUTE))).thenReturn(108000); // 1.5 hours

    PlaytimeReader reader = new PlaytimeReader(2);
    assertEquals("90m", reader.getPlaytime(player));
}
```

- [ ] **Step 11: Run test to verify it passes**

Run: `gradle test --tests '*PlaytimeReaderTest.testCustomThresholdShowsMinutes'`

Expected: PASS

- [ ] **Step 12: Commit PlaytimeReader**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/display/PlaytimeReader.java src/test/java/com/xiaohunao/iplocationdisplay/display/PlaytimeReaderTest.java build.gradle
git commit -m "feat: add PlaytimeReader for stats conversion"
```

## Task 2: Extend DisplayTextFormatter for Playtime Placeholder

**Files:**
- Modify: `src/main/java/com/xiaohunao/iplocationdisplay/display/DisplayTextFormatter.java`
- Modify: `src/test/java/com/xiaohunao/iplocationdisplay/display/DisplayTextFormatterTest.java`

**Interfaces:**
- Consumes: Nothing (extends existing formatter)
- Produces: `String format(String displayFormat, String location, String playtime)` method

- [ ] **Step 1: Write failing test for playtime placeholder**

Add to `DisplayTextFormatterTest.java`:

```java
@Test
public void testFormatWithPlaytime() {
    DisplayTextFormatter formatter = new DisplayTextFormatter();
    String result = formatter.format("%location% %playtime%", "广东", "120h");
    assertEquals("广东 120h", result);
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests '*DisplayTextFormatterTest.testFormatWithPlaytime'`

Expected: Compilation error - `format()` method does not accept 3 parameters

- [ ] **Step 3: Update DisplayTextFormatter to accept playtime parameter**

Replace the `format` method in `DisplayTextFormatter.java`:

```java
public String format(String displayFormat, String location, String playtime) {
    String format = displayFormat == null || displayFormat.isBlank() ? "%location%" : displayFormat;
    String locationValue = location == null ? "" : location;
    String playtimeValue = playtime == null ? "" : playtime;
    return format
            .replace("%location%", locationValue)
            .replace("%playtime%", playtimeValue)
            .trim();
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle test --tests '*DisplayTextFormatterTest.testFormatWithPlaytime'`

Expected: PASS

- [ ] **Step 5: Write test for location-only format**

Add to `DisplayTextFormatterTest.java`:

```java
@Test
public void testFormatLocationOnly() {
    DisplayTextFormatter formatter = new DisplayTextFormatter();
    String result = formatter.format("%location%", "广东", "120h");
    assertEquals("广东", result);
}
```

- [ ] **Step 6: Run test to verify it passes**

Run: `gradle test --tests '*DisplayTextFormatterTest.testFormatLocationOnly'`

Expected: PASS

- [ ] **Step 7: Write test for empty playtime**

Add to `DisplayTextFormatterTest.java`:

```java
@Test
public void testFormatWithEmptyPlaytime() {
    DisplayTextFormatter formatter = new DisplayTextFormatter();
    String result = formatter.format("%location% %playtime%", "广东", "");
    assertEquals("广东", result);
}
```

- [ ] **Step 8: Run test to verify it passes**

Run: `gradle test --tests '*DisplayTextFormatterTest.testFormatWithEmptyPlaytime'`

Expected: PASS

- [ ] **Step 9: Write test for no placeholders**

Add to `DisplayTextFormatterTest.java`:

```java
@Test
public void testFormatWithNoPlaceholders() {
    DisplayTextFormatter formatter = new DisplayTextFormatter();
    String result = formatter.format("[固定文本]", "广东", "120h");
    assertEquals("[固定文本]", result);
}
```

- [ ] **Step 10: Run test to verify it passes**

Run: `gradle test --tests '*DisplayTextFormatterTest.testFormatWithNoPlaceholders'`

Expected: PASS

- [ ] **Step 11: Update existing test that uses old signature**

Find and update the existing test in `DisplayTextFormatterTest.java` that calls `format()` with 2 parameters. Change it to pass empty string as third parameter:

```java
// Old: formatter.format(template, location)
// New: formatter.format(template, location, "")
```

- [ ] **Step 12: Run all DisplayTextFormatter tests**

Run: `gradle test --tests '*DisplayTextFormatterTest'`

Expected: All tests PASS

- [ ] **Step 13: Commit DisplayTextFormatter changes**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/display/DisplayTextFormatter.java src/test/java/com/xiaohunao/iplocationdisplay/display/DisplayTextFormatterTest.java
git commit -m "feat: add playtime placeholder to DisplayTextFormatter"
```

## Task 3: Add Playtime Config Fields

**Files:**
- Modify: `src/main/java/com/xiaohunao/iplocationdisplay/config/IpLocationConfig.java`
- Test: `src/test/java/com/xiaohunao/iplocationdisplay/config/IpLocationConfigTest.java`

**Interfaces:**
- Consumes: Nothing (config definition)
- Produces: `SHOW_PLAYTIME` (BooleanValue), `PLAYTIME_HOUR_THRESHOLD` (IntValue), updated `RuntimeSettings` record with `boolean showPlaytime()` and `int playtimeHourThreshold()` fields

- [ ] **Step 1: Write failing test for config defaults**

Add to `IpLocationConfigTest.java` (create file if it doesn't exist):

```java
package com.xiaohunao.iplocationdisplay.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class IpLocationConfigTest {
    @Test
    public void testShowPlaytimeDefault() {
        assertTrue(IpLocationConfig.SHOW_PLAYTIME.get());
    }

    @Test
    public void testPlaytimeHourThresholdDefault() {
        assertEquals(1, IpLocationConfig.PLAYTIME_HOUR_THRESHOLD.get());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradle test --tests '*IpLocationConfigTest'`

Expected: Compilation error - `SHOW_PLAYTIME` and `PLAYTIME_HOUR_THRESHOLD` do not exist

- [ ] **Step 3: Add SHOW_PLAYTIME and PLAYTIME_HOUR_THRESHOLD config fields**

Add after the `TICK_INTERVAL` field in `IpLocationConfig.java`:

```java
public static final ModConfigSpec.BooleanValue SHOW_PLAYTIME = BUILDER
        .comment("Show player playtime in the head display.")
        .define("showPlaytime", true);
public static final ModConfigSpec.IntValue PLAYTIME_HOUR_THRESHOLD = BUILDER
        .comment("Show minutes when playtime is below this hour threshold.")
        .defineInRange("playtimeHourThreshold", 1, 0, 100);
```

- [ ] **Step 4: Run test to verify it passes**

Run: `gradle test --tests '*IpLocationConfigTest'`

Expected: PASS

- [ ] **Step 5: Update displayFormat default to include playtime**

Find the `DISPLAY_FORMAT` field definition and update the default value:

```java
public static final ModConfigSpec.ConfigValue<String> DISPLAY_FORMAT = BUILDER
        .comment("Text format for the head display. Use %location% for the resolved location and %playtime% for play time.")
        .define("displayFormat", "%location% %playtime%");
```

- [ ] **Step 6: Add fields to RuntimeSettings record**

Find the `RuntimeSettings` record definition and add two new fields at the end:

```java
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
        int httpTimeoutMillis,
        boolean showPlaytime,
        int playtimeHourThreshold
) {}
```

- [ ] **Step 7: Update runtimeSettings() method to include new fields**

Find the `runtimeSettings()` method and add the new fields at the end:

```java
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
            HTTP_TIMEOUT_MILLIS.get(),
            SHOW_PLAYTIME.get(),
            PLAYTIME_HOUR_THRESHOLD.get()
    );
}
```

- [ ] **Step 8: Build to verify config compiles**

Run: `gradle build`

Expected: BUILD SUCCESSFUL

- [ ] **Step 9: Commit config changes**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/config/IpLocationConfig.java src/test/java/com/xiaohunao/iplocationdisplay/config/IpLocationConfigTest.java
git commit -m "feat: add playtime config fields"
```

## Task 4: Wire Playtime into PlayerDisplayManager

**Files:**
- Modify: `src/main/java/com/xiaohunao/iplocationdisplay/display/PlayerDisplayManager.java`

**Interfaces:**
- Consumes: `PlaytimeReader(int)` constructor and `String getPlaytime(ServerPlayer)`, `DisplayTextFormatter.format(String, String, String)`
- Produces: Complete playtime display feature

- [ ] **Step 1: Add PlaytimeReader field to PlayerDisplayManager**

Add after the `displayTextFormatter` field:

```java
private final PlaytimeReader playtimeReader;
```

- [ ] **Step 2: Initialize PlaytimeReader in constructor**

Add after `this.settings = settings;`:

```java
this.playtimeReader = new PlaytimeReader(settings.playtimeHourThreshold());
```

- [ ] **Step 3: Find the createOrReplace method**

Locate the `createOrReplace(ServerPlayer player, IpLocation ipLocation)` method around line 125.

- [ ] **Step 4: Add playtime reading before text formatting**

Find the line that calls `displayTextFormatter.format()` (around line 136). Add playtime reading before it:

```java
String playtimeStr = settings.showPlaytime() 
    ? playtimeReader.getPlaytime(player)
    : "";

String text = displayTextFormatter.format(settings.displayFormat(), ipLocation.value(), playtimeStr);
```

- [ ] **Step 5: Build to verify integration compiles**

Run: `gradle build`

Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Run all tests**

Run: `gradle test`

Expected: All tests PASS

- [ ] **Step 7: Commit PlayerDisplayManager changes**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/display/PlayerDisplayManager.java
git commit -m "feat: integrate playtime display into head labels"
```

## Task 5: Manual Verification and Documentation

**Files:**
- Modify: `README.md`

**Interfaces:**
- Consumes: Complete implemented feature
- Produces: Updated documentation

- [ ] **Step 1: Build the mod jar**

Run: `gradle build`

Expected: BUILD SUCCESSFUL, jar created in `build/libs/`

- [ ] **Step 2: Update README with playtime feature**

Add a new section after the "Config Examples" section:

```markdown
## Playtime Display

The mod can show each player's total playtime alongside their location.

Default format: `中国 广东 广州 120h`

### Config

```toml
# Show playtime in display
showPlaytime = true

# Show minutes when playtime < this hour threshold
playtimeHourThreshold = 1

# Use %playtime% placeholder in displayFormat
displayFormat = "%location% %playtime%"
```

### Examples

Show location and playtime (default):
```toml
displayFormat = "%location% %playtime%"
showPlaytime = true
```
Result: `中国 广东 广州 120h`

Show only location:
```toml
displayFormat = "%location%"
showPlaytime = false
```
Result: `中国 广东 广州`

Custom format:
```toml
displayFormat = "[%location% | 游玩: %playtime%]"
```
Result: `[中国 广东 广州 | 游玩: 120h]`

### Time Display Rules

- Playtime < 1 hour: shows minutes (e.g., `45m`)
- Playtime ≥ 1 hour: shows hours (e.g., `120h`)
- Threshold is configurable via `playtimeHourThreshold`
```

- [ ] **Step 3: Commit README update**

```bash
git add README.md
git commit -m "docs: document playtime display feature"
```

- [ ] **Step 4: Manual smoke test (if server available)**

If a test server is available:
1. Copy jar to server `mods/` directory
2. Start server
3. Join with player
4. Verify display shows location and playtime
5. Edit `config/laoxiang.toml` and set `showPlaytime = false`
6. Restart or reload config
7. Verify playtime no longer shows

If server not available, skip this step.

- [ ] **Step 5: Push all commits to remote**

```bash
git push
```

Expected: All commits pushed successfully

## Self-Review

**Spec coverage:**
- ✅ PlaytimeReader reads `Stats.PLAY_ONE_MINUTE` - Task 1
- ✅ Converts ticks to hours/minutes - Task 1
- ✅ DisplayTextFormatter supports `%playtime%` - Task 2
- ✅ Config fields for `showPlaytime` and `playtimeHourThreshold` - Task 3
- ✅ Updated default `displayFormat` - Task 3
- ✅ Integrated into PlayerDisplayManager - Task 4
- ✅ Unit tests for all components - Tasks 1-3
- ✅ Documentation updated - Task 5

**Placeholder scan:**
- No TBD or TODO markers
- All code examples complete
- All test cases have expected values
- All commands have expected output

**Type consistency:**
- `PlaytimeReader.getPlaytime()` returns `String` - consistent across all tasks
- `DisplayTextFormatter.format()` signature matches everywhere: `(String, String, String)`
- Config fields: `showPlaytime` (boolean), `playtimeHourThreshold` (int) - consistent
- RuntimeSettings fields match config getters
