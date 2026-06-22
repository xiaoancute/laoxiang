# IP Location Display

NeoForge 1.21.1 server-side mod that shows a player's IP location above their head using vanilla `minecraft:text_display` entities.

## Install

1. Build the mod from GitHub Actions or download the workflow artifact.
2. Put the jar in the NeoForge server `mods` directory.
3. Start the server once to generate the server config.

Clients do not need to install this mod.

## Default Lookup

The default provider is online lookup through:

```text
http://ip-api.com/json/%ip%?lang=zh-CN&fields=status,message,country,regionName,city,query
```

The default displayed location template is:

```text
%country% %regionName% %city%
```

IP location is approximate. Mobile networks, VPNs, proxies, cloud providers, campus networks, and recently reassigned ISP ranges can show inaccurate locations.

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
httpLocationTemplate = "%data.country% %data.region% %data.city%"
httpTimeoutMillis = 2000
```

## Display Behavior

- Local/private addresses display `localText`.
- Unknown public locations are hidden by default.
- Set `showUnknown = true` to display `unknownText`.
- Display position updates every `tickInterval` server ticks.
- The display is removed when a player leaves or changes dimension.
