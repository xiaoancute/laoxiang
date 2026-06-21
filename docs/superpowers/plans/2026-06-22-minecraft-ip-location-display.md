# Minecraft IP Location Display Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a NeoForge 1.21.1 server-side mod that shows each player's IP location above their head using vanilla `TextDisplay` entities.

**Architecture:** Pure Java services handle IP parsing, provider selection, HTTP/local lookup, caching, and text formatting. NeoForge-specific classes only wire server events, config, and display entity lifecycle. Runtime lookup is asynchronous so joins and ticks never block on HTTP.

**Tech Stack:** Java 21, Gradle, NeoForge 21.1.234, ModDevGradle 2.0.141, JUnit 5, Gson, `org.lionsoul:ip2region:2.7.0`, vanilla `TextDisplay` entities.

---

## File Structure

- `settings.gradle`: Gradle plugin repositories and root name.
- `build.gradle`: Java, NeoForge, JUnit, Gson, and ip2region setup.
- `gradle.properties`: mod metadata and dependency versions.
- `src/main/resources/META-INF/neoforge.mods.toml`: NeoForge mod metadata.
- `src/main/java/com/xiaohunao/iplocationdisplay/IpLocationDisplayMod.java`: mod bootstrap, config registration, event wiring.
- `src/main/java/com/xiaohunao/iplocationdisplay/config/IpLocationConfig.java`: NeoForge server config values and conversion to runtime settings.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/AddressNormalizer.java`: remote address parsing and local/private address detection.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/IpLocation.java`: resolved location value object.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/LocationProvider.java`: lookup provider interface.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/LocationProviders.java`: local/http/hybrid provider selection.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/CachedLocationResolver.java`: async cache and fallback handling.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/Ip2RegionLocationProvider.java`: optional local `ip2region.xdb` lookup.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/HttpLocationProvider.java`: configurable HTTP lookup.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/HttpLookupClient.java`: HTTP abstraction for tests.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/JdkHttpLookupClient.java`: Java `HttpClient` implementation.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/JsonPathReader.java`: tiny dotted JSON field reader.
- `src/main/java/com/xiaohunao/iplocationdisplay/location/LocationTemplateFormatter.java`: `%field%` formatter for HTTP location templates.
- `src/main/java/com/xiaohunao/iplocationdisplay/display/DisplayTextFormatter.java`: `%location%` display formatting.
- `src/main/java/com/xiaohunao/iplocationdisplay/display/PlayerDisplayManager.java`: `TextDisplay` spawn, move, dimension change, and cleanup.
- `src/test/java/com/xiaohunao/iplocationdisplay/location/*Test.java`: pure lookup tests.
- `src/test/java/com/xiaohunao/iplocationdisplay/display/DisplayTextFormatterTest.java`: display format tests.

## Task 1: Project Scaffold

**Files:**
- Create: `settings.gradle`
- Create: `build.gradle`
- Create: `gradle.properties`
- Create: `src/main/resources/META-INF/neoforge.mods.toml`

- [ ] **Step 1: Create Gradle scaffold**

Use NeoForge 1.21.1 and Java 21. The project starts without mod code so dependency resolution can be verified first.

```groovy
// settings.gradle
pluginManagement {
    repositories {
        mavenLocal()
        maven { url = 'https://maven.neoforged.net/releases' }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = 'https://maven.neoforged.net/releases' }
    }
}

rootProject.name = 'minecraft-ip-location-display'
```

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2G
org.gradle.daemon=false

minecraft_version=1.21.1
neoforge_version=21.1.234
mod_id=iplocationdisplay
mod_name=IP Location Display
mod_license=MIT
mod_version=0.1.0
mod_group_id=com.xiaohunao
mod_authors=xiaohunao
mod_description=Shows player IP locations above player heads on NeoForge servers.
```

- [ ] **Step 2: Run dependency verification**

Run: `./gradlew --version`

Expected: Gradle runs with Java 21 available. If the wrapper is not present, run system `gradle wrapper --gradle-version 8.10.2` first, then use `./gradlew`.

- [ ] **Step 3: Commit scaffold**

```bash
git add settings.gradle build.gradle gradle.properties src/main/resources/META-INF/neoforge.mods.toml gradlew gradlew.bat gradle/wrapper
git commit -m "build: scaffold NeoForge mod project"
```

## Task 2: IP Address Normalization

**Files:**
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/AddressNormalizer.java`
- Test: `src/test/java/com/xiaohunao/iplocationdisplay/location/AddressNormalizerTest.java`

- [ ] **Step 1: Write failing tests**

Cover IPv4 with port, bracketed IPv6, slash-prefixed socket strings, localhost, RFC1918, link-local, and public addresses.

Run: `./gradlew test --tests '*AddressNormalizerTest'`

Expected before implementation: compile failure because `AddressNormalizer` does not exist.

- [ ] **Step 2: Implement `AddressNormalizer`**

Implement:

```java
public final class AddressNormalizer {
    public Optional<String> normalize(String rawAddress);
    public boolean isLocalOrPrivate(String ip);
}
```

Rules:

- `"/1.2.3.4:51234"` becomes `1.2.3.4`.
- `"1.2.3.4:51234"` becomes `1.2.3.4`.
- `"/[2001:4860:4860::8888]:51234"` becomes `2001:4860:4860::8888`.
- `127.0.0.1`, `10.0.0.1`, `172.16.0.1`, `192.168.1.1`, `::1`, and link-local addresses are local/private.
- Invalid or blank input returns `Optional.empty()`.

- [ ] **Step 3: Verify tests pass**

Run: `./gradlew test --tests '*AddressNormalizerTest'`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/location/AddressNormalizer.java src/test/java/com/xiaohunao/iplocationdisplay/location/AddressNormalizerTest.java
git commit -m "feat: normalize player remote addresses"
```

## Task 3: Formatting Helpers

**Files:**
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/JsonPathReader.java`
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/LocationTemplateFormatter.java`
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/display/DisplayTextFormatter.java`
- Test: matching `*Test.java` files under `src/test/java`.

- [ ] **Step 1: Write failing formatting tests**

Tests:

- `JsonPathReader` reads `status` and `data.region`.
- missing paths return empty.
- `LocationTemplateFormatter` formats `%country% %regionName% %city%` and collapses whitespace.
- missing template values are omitted.
- `DisplayTextFormatter` turns `[%location%]` and `Guangdong` into `[Guangdong]`.

Run: `./gradlew test --tests '*JsonPathReaderTest' --tests '*LocationTemplateFormatterTest' --tests '*DisplayTextFormatterTest'`

Expected before implementation: compile failure for missing classes.

- [ ] **Step 2: Implement helpers**

Public APIs:

```java
public final class JsonPathReader {
    public Optional<String> read(JsonObject root, String path);
}

public final class LocationTemplateFormatter {
    public String format(JsonObject root, String template);
}

public final class DisplayTextFormatter {
    public String format(String displayFormat, String location);
}
```

- [ ] **Step 3: Verify tests pass**

Run: `./gradlew test --tests '*JsonPathReaderTest' --tests '*LocationTemplateFormatterTest' --tests '*DisplayTextFormatterTest'`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/location src/main/java/com/xiaohunao/iplocationdisplay/display src/test/java
git commit -m "feat: add location formatting helpers"
```

## Task 4: HTTP Provider

**Files:**
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/IpLocation.java`
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/LocationProvider.java`
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/HttpLookupClient.java`
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/HttpLocationProvider.java`
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/JdkHttpLookupClient.java`
- Test: `src/test/java/com/xiaohunao/iplocationdisplay/location/HttpLocationProviderTest.java`

- [ ] **Step 1: Write failing provider tests**

Cover:

- success JSON from `ip-api.com` returns `美国 弗吉尼亚州 Ashburn`.
- URL template replaces `%ip%`.
- `status=fail`, HTTP 429, invalid JSON, empty formatted location, and timeout exceptions return empty.

Run: `./gradlew test --tests '*HttpLocationProviderTest'`

Expected before implementation: compile failure for missing provider classes.

- [ ] **Step 2: Implement provider interfaces**

Public APIs:

```java
public record IpLocation(String value) {}

public interface LocationProvider {
    Optional<IpLocation> lookup(String ip) throws Exception;
}

public interface HttpLookupClient {
    HttpLookupResponse get(String url, Duration timeout) throws Exception;
}

public record HttpLookupResponse(int statusCode, String body) {}
```

`HttpLocationProvider` accepts URL template, success path, success value, location template, timeout, `HttpLookupClient`, `JsonPathReader`, and `LocationTemplateFormatter`.

- [ ] **Step 3: Verify tests pass**

Run: `./gradlew test --tests '*HttpLocationProviderTest'`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/location src/test/java/com/xiaohunao/iplocationdisplay/location/HttpLocationProviderTest.java
git commit -m "feat: resolve locations from HTTP provider"
```

## Task 5: Local Provider and Provider Selection

**Files:**
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/Ip2RegionLocationProvider.java`
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/ProviderMode.java`
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/LocationProviders.java`
- Test: provider tests under `src/test/java/com/xiaohunao/iplocationdisplay/location`.

- [ ] **Step 1: Write failing selection tests**

Cover:

- `http` mode uses only HTTP provider.
- `local` mode uses only local provider.
- `hybrid` mode returns local result first.
- `hybrid` mode falls back to HTTP when local returns empty.
- local provider returns empty when database path is missing.

Run: `./gradlew test --tests '*LocationProvidersTest' --tests '*Ip2RegionLocationProviderTest'`

Expected before implementation: compile failure for missing classes.

- [ ] **Step 2: Implement local provider and selection**

`Ip2RegionLocationProvider` checks `Files.isRegularFile(databasePath)` before creating a searcher. It returns empty when the file is absent.

`LocationProviders` exposes:

```java
public static LocationProvider choose(ProviderMode mode, LocationProvider local, LocationProvider http);
```

- [ ] **Step 3: Verify tests pass**

Run: `./gradlew test --tests '*LocationProvidersTest' --tests '*Ip2RegionLocationProviderTest'`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/location src/test/java/com/xiaohunao/iplocationdisplay/location
git commit -m "feat: select local and HTTP location providers"
```

## Task 6: Cached Async Resolver

**Files:**
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/location/CachedLocationResolver.java`
- Test: `src/test/java/com/xiaohunao/iplocationdisplay/location/CachedLocationResolverTest.java`

- [ ] **Step 1: Write failing resolver tests**

Cover:

- local/private IPs return `localText`.
- public IPs use provider once and then cache the result.
- failures are cached as empty.
- unknown text is returned only when `showUnknown=true`.
- lookups return `CompletableFuture<Optional<IpLocation>>`.

Run: `./gradlew test --tests '*CachedLocationResolverTest'`

Expected before implementation: compile failure for missing resolver.

- [ ] **Step 2: Implement resolver**

Constructor inputs: `AddressNormalizer`, `LocationProvider`, executor, `localText`, `unknownText`, and `showUnknown`.

Public API:

```java
public CompletableFuture<Optional<IpLocation>> resolve(String rawAddress);
public void shutdown();
```

- [ ] **Step 3: Verify tests pass**

Run: `./gradlew test --tests '*CachedLocationResolverTest'`

Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/location/CachedLocationResolver.java src/test/java/com/xiaohunao/iplocationdisplay/location/CachedLocationResolverTest.java
git commit -m "feat: cache asynchronous IP location lookups"
```

## Task 7: NeoForge Config and Bootstrap

**Files:**
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/config/IpLocationConfig.java`
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/IpLocationDisplayMod.java`
- Modify: `src/main/resources/META-INF/neoforge.mods.toml`

- [ ] **Step 1: Add NeoForge config**

Register `ModConfig.Type.SERVER` values matching the spec:

- `enabled`
- `providerMode`
- `httpPreset`
- `displayFormat`
- `verticalOffset`
- `tickInterval`
- `showUnknown`
- `unknownText`
- `localText`
- `databasePath`
- `httpUrlTemplate`
- `httpSuccessJsonPath`
- `httpSuccessValue`
- `httpLocationTemplate`
- `httpTimeoutMillis`

- [ ] **Step 2: Add bootstrap wiring**

`IpLocationDisplayMod` registers config and server event handlers. It creates provider/resolver/display manager on server start and shuts them down on server stop.

- [ ] **Step 3: Build**

Run: `./gradlew build`

Expected: build succeeds.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/config src/main/java/com/xiaohunao/iplocationdisplay/IpLocationDisplayMod.java src/main/resources/META-INF/neoforge.mods.toml
git commit -m "feat: wire NeoForge config and lifecycle"
```

## Task 8: TextDisplay Manager

**Files:**
- Create: `src/main/java/com/xiaohunao/iplocationdisplay/display/PlayerDisplayManager.java`
- Modify: `src/main/java/com/xiaohunao/iplocationdisplay/IpLocationDisplayMod.java`

- [ ] **Step 1: Implement display lifecycle**

Runtime behavior:

- On player login, resolve location from remote address.
- When resolution completes, schedule entity creation back on the server thread.
- Spawn one `TextDisplay` per tracked player.
- Set billboard to center, text color gold, shadow enabled, see-through disabled, dark translucent background, no persistence, and no gravity.
- Move the display every `tickInterval` ticks to `player.position + verticalOffset`.
- On dimension change, discard the old display and create a new display in the new level.
- On logout/server stop, discard display entities and clear maps.

- [ ] **Step 2: Build**

Run: `./gradlew build`

Expected: build succeeds.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/xiaohunao/iplocationdisplay/display/PlayerDisplayManager.java src/main/java/com/xiaohunao/iplocationdisplay/IpLocationDisplayMod.java
git commit -m "feat: show IP locations above players"
```

## Task 9: Documentation and Runtime Verification

**Files:**
- Create: `README.md`

- [ ] **Step 1: Document usage**

Include:

- NeoForge 1.21.1 server-only installation.
- Default online provider behavior.
- Privacy note about online lookup.
- Local database path for `ip2region.xdb`.
- Config examples for `http`, `local`, and `hybrid`.

- [ ] **Step 2: Run full verification**

Run:

```bash
./gradlew test
./gradlew build
```

Expected: both commands pass.

- [ ] **Step 3: Commit**

```bash
git add README.md
git commit -m "docs: document IP location display mod"
```

## Self-Review

- Spec coverage: The tasks cover NeoForge 1.21.1 scaffold, online default provider, optional local provider, hybrid mode, async cache, local/private fallback, configurable text, `TextDisplay` lifecycle, cleanup, and verification.
- Placeholder scan: Every task names its provider, path, config name, command, and behavior.
- Type consistency: Provider APIs use `Optional<IpLocation>` throughout; config names match the design document; display formatting uses `%location%`; HTTP formatting uses JSON-derived `%field%` tokens.
