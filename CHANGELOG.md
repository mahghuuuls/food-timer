# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-08-14

### Added
- Initial release of **Food Timer** for Minecraft 1.12.2 (Forge and CleanroomMC).
- Configurable per-item cooldowns on food consumption.
- Independent metadata variant support (e.g. standard Golden Apple at 60s vs Enchanted Golden Apple at 300s).
- Visual radial cooldown sweep overlay on hotbar and inventory container slots.
- Item hover tooltips displaying formatted cooldown durations.
- Persistent cooldown tracking across world reloads, server restarts, and player reconnections.
- Client-server network synchronization ensuring consistent timers in multiplayer.