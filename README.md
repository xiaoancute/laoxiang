# 老乡 (LaoXiang)

NeoForge 1.21.1 server-side mod that shows a player's IP location above their head using vanilla `minecraft:text_display` entities.

## Install

1. Build the mod from GitHub Actions or download the workflow artifact.
2. Put the jar in the NeoForge server `mods` directory.
3. Start the server once to generate `config/iplocationdisplay.toml`.

Clients do not need to install this mod.

## Default Lookup

The default provider is online lookup through:

```text
https://api.ip.sb/geoip/%ip%
```

The default displayed location template is:

```text
%country_localized% %region_localized% %city_localized% %isp_localized%
```

`%isp_localized%` normalizes common Chinese network names to `电信`, `移动`, `联通`, `广电`, or `教育网` when the lookup provider returns ISP/ASN fields. Other networks fall back to the provider's ISP name, such as `Google LLC` or `Comcast Cable`.

IP location is approximate. Mobile networks, VPNs, proxies, cloud providers, campus networks, and recently reassigned ISP ranges can show inaccurate locations.

The config file is generated at `<server_folder>/config/iplocationdisplay.toml`.

## Privacy

With the default `providerMode=http`, the server sends player IPs to the configured lookup provider.

If you do not want that, switch to local lookup:

```toml
providerMode = "local"
databasePath = "config/iplocationdisplay/ip2region.xdb"
```

The local mode requires an `ip2region.xdb` database file at the configured path. The mod does not bundle a database.

## Config Examples

Online lookup:

```toml
enabled = true
providerMode = "http"
httpPreset = "ip-sb"
displayFormat = "[%location%]"
showUnknown = false
```

Local lookup:

```toml
enabled = true
providerMode = "local"
databasePath = "config/iplocationdisplay/ip2region.xdb"
localText = "Local"
```

Hybrid lookup:

```toml
enabled = true
providerMode = "hybrid"
databasePath = "config/iplocationdisplay/ip2region.xdb"
```

Custom HTTP provider:

```toml
providerMode = "http"
httpPreset = "custom"
httpUrlTemplate = "https://example.com/lookup?ip=%ip%"
httpSuccessJsonPath = "status"
httpSuccessValue = "success"
httpLocationTemplate = "%data.country% %data.region% %data.city% %isp_localized%"
httpTimeoutMillis = 2000
```

## Playtime Display

The mod can show each player's total playtime alongside their location.

Default format: `中国 广东 广州 电信 120h`

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
Result: `中国 广东 广州 电信 120h`

Show only location:
```toml
displayFormat = "%location%"
showPlaytime = false
```
Result: `中国 广东 广州 电信`

Custom format:
```toml
displayFormat = "[%location% | 游玩: %playtime%]"
```
Result: `[中国 广东 广州 电信 | 游玩: 120h]`

### Time Display Rules

- Playtime < 1 hour: shows minutes (e.g., `45m`)
- Playtime ≥ 1 hour: shows hours (e.g., `120h`)
- Threshold is configurable via `playtimeHourThreshold`

## Display Behavior

- Local/private addresses display `localText`.
- Unknown public locations are hidden by default.
- Set `showUnknown = true` to display `unknownText`.
- Display position updates every `tickInterval` server ticks. Keep `tickInterval = 1` for the most responsive tracking.
- The display is removed when a player leaves or changes dimension.
