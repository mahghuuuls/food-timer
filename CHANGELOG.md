# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-08-24

### Added
- Configurable cooldown policies for configured foods only, every standard food with a fixed duration, or every standard food scaled by restored hunger points.
- Exact-metadata and wildcard food rules that override all-food policies, including zero-second exclusions.
- Server-authoritative policy synchronization so multiplayer tooltips reflect the server's cooldown rules while retaining each client's local tooltip prefix and visibility settings.

### Changed
- Expanded generated configuration comments to explain each policy, its related values, override precedence, and zero-second exclusions.

### Fixed
- Active cooldowns remain enforceable across restarts and later policy or rule changes.
- Client reconnects safely clear and replace synchronized server policy state.
- Dedicated servers synchronize policy without loading client-only classes.
- Removed inactive Mixin manifest declarations that could cause startup failures in Mixin-enabled modpacks.

## [1.0.0] - 2026-08-14

### Added
- Initial release of **Food Timer** for Minecraft 1.12.2 (Forge and CleanroomMC).
- Configurable per-item cooldowns on food consumption.
- Independent metadata variant support (e.g. standard Golden Apple at 60s vs Enchanted Golden Apple at 300s).
- Visual cooldown overlay on hotbar and inventory container slots.
- Item hover tooltips displaying formatted cooldown durations.
- Persistent cooldown tracking across world reloads, server restarts, and player reconnections.
- Client-server network synchronization ensuring consistent timers in multiplayer.
