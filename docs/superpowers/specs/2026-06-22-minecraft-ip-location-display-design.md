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

The first implementation should use a small provider abstraction so tests can use an in-memory fake provider. The production provider uses `org.lionsoul:ip2region:2.7.0` with an optional local `ip2region.xdb` file. The default database path is `config/iplocationdisplay/ip2region.xdb`.

If the database file is missing or a public IP cannot be resolved, the display is hidden by default. Local and private addresses use the configured local text so local testing still shows a visible label.

### Head display manager

- Create one `TextDisplay` entity per tracked player after the player's location is known.
- Position it above the player's head with configurable vertical offset.
- Keep the display aligned to the player's current dimension and position.
- Remove the display when the player disconnects, dies if needed, changes dimension, or when the mod/server stops.
- Never persist these display entities to world saves.

### Configuration

Use a NeoForge server config with these initial options:

- `enabled`: global enable switch.
- `displayFormat`: default `[%location%]`.
- `verticalOffset`: default `2.6`.
- `tickInterval`: update frequency, default `5` ticks.
- `showUnknown`: default `false`.
- `unknownText`: default `Unknown`.
- `localText`: default `Local`.
- `databasePath`: default `config/iplocationdisplay/ip2region.xdb`.

### Error handling

- Lookup failures should not block login.
- Resolver errors are logged once per IP.
- If a player's address cannot be read, use `unknownText` when `showUnknown` is true; otherwise hide the display.
- If display entity creation fails, log and continue without kicking the player.

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
3. The resolver returns a location string or a fallback.
4. The display manager spawns a `TextDisplay` entity above the player.
5. On server ticks, the display manager updates the entity position.
6. On logout, dimension change, or shutdown, the display manager removes stale entities.

## Testing

Implementation will be test-first for pure logic:

- IP extraction and normalization.
- Private/local IP detection.
- Resolver cache behavior.
- Display text formatting.
- Tracking lifecycle decisions in the display manager where possible without a live server.

Runtime verification should include:

- `./gradlew test`
- `./gradlew build`
- Local NeoForge server run when the Gradle setup supports it.
