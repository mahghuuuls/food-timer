# Food Timer

A lightweight Minecraft 1.12.2 mod that adds configurable item cooldowns and duration tooltips to foods and consumables, preventing food spamming in combat and survival.

## Features

- **Configurable Item Cooldowns**: Enforces a per-player cooldown after consuming configured foods or consumables.
- **Variant & Metadata Support**: Distinguishes items with shared IDs via metadata (e.g., standard Golden Apple vs. Enchanted Golden Apple).
- **Native Visual Feedback**: Displays Minecraft's native radial cooldown sweep overlay on item stacks.
- **Duration Tooltips**: Appends formatted cooldown duration details to hovered items.
- **Zero External Dependencies**: Standard Forge 1.12.2 and Cleanroom compatible without third-party APIs.

## Default Configuration

- **Golden Apple (`minecraft:golden_apple:0`)**: 60 seconds (1 minute)
- **Enchanted Golden Apple (`minecraft:golden_apple:1`)**: 300 seconds (5 minutes)
- All other items have no cooldown unless added to `config/foodtimer.cfg`.

## Building from Source

This project uses RetroFuturaGradle and Gradle wrapper.

```bash
# Linux / macOS
./gradlew clean build

# Windows
.\gradlew.bat clean build
```

Built artifacts will be placed in `build/libs/`.

## License

This project is licensed under the [MIT License](LICENSE). Third-party template notices are in [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md).
