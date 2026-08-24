# Food Timer

Food Timer is a Minecraft 1.12.2 mod that adds server-enforced consumption cooldowns, duration tooltips, and cooldown overlays to foods and configured consumables.

Releases are available on [CurseForge](https://www.curseforge.com/minecraft/mc-mods/food-timer). The source repository is [mahghuuuls/food-timer](https://github.com/mahghuuuls/food-timer).

## Features

- Three cooldown policies: configured items only, one fixed duration for all standard foods, or a duration scaled by restored hunger points.
- Exact-metadata and wildcard rules that override the selected all-food policy. A zero-second rule excludes the matching item.
- Per-player cooldowns that remain active across reconnects, respawns, world reloads, and server restarts.
- Server-authoritative multiplayer behavior and tooltips.
- Native-style cooldown overlays in the hotbar and inventory, plus configurable duration tooltips.
- Standard Forge and Cleanroom support without third-party runtime dependencies. The verified baselines are Forge 14.23.5.2847 and Cleanroom 0.6.11-alpha; other permitted Minecraft 1.12.2 Forge builds have not all been individually verified.

## Installation

Install the mod on both the client and server. For singleplayer, install it in the client instance.

## Configuration

Food Timer creates `config/foodtimer.cfg`. Gameplay configuration is owned by the logical server, and changes require a server or integrated-server restart.

| Key | Purpose |
| --- | --- |
| `cooldownPolicy` | `CONFIGURED_ONLY` keeps the original behavior. `FIXED_ALL_FOODS` applies one duration to standard foods. `SCALED_ALL_FOODS` multiplies restored hunger points by a configured number of seconds. |
| `fixedFoodCooldownSeconds` | Global duration used by `FIXED_ALL_FOODS` when no item rule matches. |
| `secondsPerHungerPoint` | Multiplier used by `SCALED_ALL_FOODS` when no item rule matches. |
| `foodCooldowns` | Rules in the form `modid:item[:metadata]=seconds`. Under all-food policies these are overrides; under `CONFIGURED_ONLY` they are the complete cooldown list. |
| `enableTooltips` | Enables or disables duration tooltips on that client. |
| `tooltipPrefix` | Sets the tooltip text prefix on that client. |
| `enableDebugLogging` | Enables bounded diagnostic logging. |

An exact metadata rule takes precedence over a wildcard rule. Omitting metadata or using `*` matches every metadata variant, and a duration of `0` excludes the match from cooldowns. The default policy is `CONFIGURED_ONLY`, with standard and enchanted Golden Apples set to 60 and 300 seconds respectively.

## Building from Source

This project uses RetroFuturaGradle and the Gradle wrapper.

```bash
# Linux / macOS
./gradlew clean build

# Windows
.\gradlew.bat clean build
```

Built artifacts are placed in `build/libs/`.

## License

This project is licensed under the [MIT License](LICENSE). Third-party template notices are in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
