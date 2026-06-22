# Repository Guidelines

## Project Structure & Module Organization

This is a Java 21 NeoForge server-side mod for Minecraft 1.21.1. Main code lives under `src/main/java/com/xiaohunao/iplocationdisplay`, split by concern:

- `config/` handles generated TOML settings.
- `display/` manages text display entities and playtime/location formatting.
- `location/` contains HTTP, local ip2region, caching, JSON path, and address normalization logic.

NeoForge metadata is in `src/main/resources/META-INF/neoforge.mods.toml`. Unit tests mirror production packages under `src/test/java`. Project notes and implementation plans are kept in `docs/`, and operational scripts are in `scripts/`.

## Build, Test, and Development Commands

This repository does not currently include a Gradle wrapper, so use a local Gradle installation with Java 21 available.

- `gradle test` runs the JUnit 5 unit test suite.
- `gradle build` compiles, tests, and creates mod artifacts in `build/libs/`.
- `gradle runServer` starts the NeoForge development server defined in `build.gradle`.
- `scripts/smoke-test-server.sh [path/to/laoxiang-*.jar]` installs a temporary NeoForge server, loads the built jar, and verifies default config generation. This script downloads NeoForge and writes under `${RUNNER_TEMP:-/tmp}`.

## Coding Style & Naming Conventions

Use 4-space indentation, UTF-8 source encoding, and standard Java naming: `PascalCase` for classes and records, `camelCase` for methods, fields, and local variables, and `UPPER_SNAKE_CASE` for constants when appropriate. Keep packages under `com.xiaohunao.iplocationdisplay`. Prefer small, focused classes that isolate Minecraft runtime code from pure logic so unit tests can run without a server.

## Testing Guidelines

Tests use JUnit Jupiter and Mockito. Name test classes with the `*Test` suffix and place them in the matching package under `src/test/java`. Favor pure unit tests for formatting, provider selection, address normalization, caching behavior, and HTTP response parsing. Avoid tests that require a live Minecraft runtime in CI; use `scripts/smoke-test-server.sh` for end-to-end server validation after building.

## Commit & Pull Request Guidelines

Recent commits use Conventional Commit-style prefixes such as `fix:` and `chore:`. Keep commit subjects imperative and specific, for example `fix: update smoke test script to match mod_id`.

Pull requests should describe the behavioral change, list validation performed (`gradle test`, `gradle build`, smoke test when relevant), and mention config or privacy impacts for lookup-provider changes. Include screenshots or logs only when display behavior or server startup output is part of the change.

## Security & Configuration Tips

The default `providerMode=http` sends player IPs to the configured lookup provider. Document any provider changes clearly and avoid committing private databases such as `ip2region.xdb` unless explicitly intended.
