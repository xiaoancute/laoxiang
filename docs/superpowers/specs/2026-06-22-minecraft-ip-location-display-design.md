# Minecraft IP Location Display Mod Design

Date: 2026-06-22

## Goal

Build a NeoForge 1.21.1 server-side-only mod that shows each online player's IP location above their head. Vanilla clients should be able to see the display without installing a client mod.

## Chosen Approach

Use vanilla `TextDisplay` entities as lightweight holograms that follow players. This follows the pattern used by server-side hologram mods/plugins: the server creates display entities, keeps them near target players, and removes them when no longer needed.

This is preferred over scoreboard team name prefixes because it can show the location as an independent line above the player instead of modifying the player's visible name.

## Non-Goals

- No client-side renderer or client-only dependency.
- No custom protocol that requires a client mod.
- No bundled third-party IP database. Server owners provide the database file in the config directory.
- No Paper/Bukkit plugin support in this implementation.

## Accuracy Expectations

IP location is approximate. It can be wrong for mobile networks, VPNs, proxies, cloud providers, campus networks, and ISP address blocks that were recently reassigned.

The mod supports both offline and online lookup modes. The default favors accuracy and uses the built-in `ip-api.com` HTTP preset. Server owners who do not want to send player IPs to a third-party lookup service can switch `providerMode` to `local`.

- Offline lookup is private, fast, and works without network access, but accuracy depends on the freshness of the local database.
- Online lookup can be more current, but sends player IPs to the configured provider and depends on provider availability, rate limits, and terms of use.
- Hybrid lookup uses the local database first and falls back to the configured online provider when local lookup returns no useful result.

## Components

### Mod bootstrap

- Target loader: NeoForge.
- Target Minecraft version: 1.21.1.
- Register server lifecycle, player login/logout, dimension change, and server tick handlers.
- Keep all implementation on the server side.

### Location resolver

- Extract the remote address from the player's server connection.
- Normalize IP addresses:
  - strip ports,
  - handle IPv4 and IPv6,
  - treat localhost/private/reserved addresses as `localText`.
- Resolve the normalized IP through a provider interface.
- Cache resolved locations by IP to avoid repeated lookups.

The first implementation uses a small provider abstraction so tests can use an in-memory fake provider. The local production provider uses `org.lionsoul:ip2region:2.7.0` with an optional local `ip2region.xdb` file. The default database path is `config/iplocationdisplay/ip2region.xdb`.

The online production provider uses a configurable HTTP URL template. The default preset calls `http://ip-api.com/json/%ip%?lang=zh-CN&fields=status,message,country,regionName,city,query` and formats the display from `%country% %regionName% %city%`. The formatter collapses extra whitespace and omits missing fields.

If the configured providers cannot resolve a public IP, the display is hidden by default. Local and private addresses use the configured local text so local testing still shows a visible label.

### Head display manager

- Create one `TextDisplay` entity per tracked player after the player's location is known.
- Position it above the player's head with configurable vertical offset.
- Keep the display aligned to the player's current dimension and position.
- Remove the display when the player disconnects, dies if needed, changes dimension, or when the mod/server stops.
- Never persist these display entities to world saves.

### Configuration

Use a NeoForge server config with these initial options:

- `enabled`: global enable switch.
- `providerMode`: default `http`; allowed values are `local`, `http`, and `hybrid`.
- `httpPreset`: default `ip-api-com`; allowed values are `ip-api-com` and `custom`.
- `displayFormat`: default `[%location%]`.
- `verticalOffset`: default `2.6`.
- `tickInterval`: update frequency, default `1` tick for smooth real-time following.
- `showUnknown`: default `false`.
- `unknownText`: default `Unknown`.
- `localText`: default `Local`.
- `databasePath`: default `config/iplocationdisplay/ip2region.xdb`.
- `httpUrlTemplate`: default `http://ip-api.com/json/%ip%?lang=zh-CN&fields=status,message,country,regionName,city,query`.
- `httpSuccessJsonPath`: default `status`.
- `httpSuccessValue`: default `success`.
- `httpLocationTemplate`: default `%country% %regionName% %city%`.
- `httpTimeoutMillis`: default `2000`.

### Error handling

- Lookup failures do not block login.
- Resolver errors are logged once per IP.
- If a player's address cannot be read, use `unknownText` when `showUnknown` is true; otherwise hide the display.
- If display entity creation fails, log and continue without kicking the player.
- HTTP lookup runs off the main server thread and times out according to `httpTimeoutMillis`.
- Successful and failed lookup results are cached by IP to reduce repeated local and HTTP work.
- HTTP 429, non-2xx responses, invalid JSON, missing success markers, and empty formatted locations count as lookup failures.

### TextDisplay style

- Text: formatted with `displayFormat`.
- Billboard: centered so the label faces viewers.
- Background: semi-transparent dark background.
- Text color: gold.
- Shadow: enabled.
- See-through: disabled so labels do not render through blocks.
- Persistence: disabled.

## Data Flow

1. Player joins the server.
2. The mod reads and normalizes the player's remote IP.
3. The resolver selects local, HTTP, or hybrid lookup from `providerMode`.
4. The resolver returns a location string or a fallback.
5. The display manager spawns a `TextDisplay` entity above the player.
6. On server ticks, the display manager updates the entity position.
7. On logout, dimension change, or shutdown, the display manager removes stale entities.

## Testing

Implementation will be test-first for pure logic:

- IP extraction and normalization.
- Private/local IP detection.
- Resolver cache behavior.
- Local, HTTP, and hybrid provider selection.
- HTTP success marker, timeout, non-2xx, invalid JSON, and missing-field behavior using a fake HTTP client.
- HTTP location template formatting with top-level and dotted JSON paths.
- Display text formatting.
- Tracking lifecycle decisions in the display manager where possible without a live server.

Runtime verification includes:

- `./gradlew test`
- `./gradlew build`
- Local NeoForge server run when the Gradle setup supports it.
