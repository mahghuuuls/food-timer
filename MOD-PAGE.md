<span style="color:#d6a100">**AI usage disclaimer:**</span> This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.

# Food Timer

**Food Timer** is a lightweight Minecraft 1.12.2 mod that adds configurable consumption cooldowns and duration tooltips to foods and consumables, preventing food spamming during combat and survival encounters.

---

## Why Food Timer?

In vanilla Minecraft, powerful foods like Golden Apples can be consumed back-to-back without limit, trivializing difficult combat situations and player vs player encounters. 

**Food Timer** introduces a clean, configurable cooldown system:
- **Independent Cooldowns**: Standard Golden Apples and Enchanted Golden Apples have completely separate timers, even though they share the same base item ID.
- **Visual Overlay**: Displays Minecraft's familiar radial sweep overlay across the item icon on your hotbar and inventory while on cooldown.
- **Item Tooltips**: Adds clear hover tooltips showing the exact cooldown duration (e.g. `Cooldown: 1m` or `Cooldown: 5m`).
- **Persistent Across Relogs**: Active cooldowns are saved to player data, preventing players from bypassing timers by disconnecting or changing worlds.
- **Universal Mod Support**: Add cooldowns to any vanilla or modded food item using simple configuration entries.

---

## Default Settings

Out of the box, Food Timer configures:
- **Golden Apple**: 60 seconds (1 minute) cooldown
- **Enchanted Golden Apple**: 300 seconds (5 minutes) cooldown
- All other vanilla and modded foods remain unhindered unless added to the configuration.

---

## Configuration

The configuration file is located at `config/foodtimer.cfg`.

### Example Configuration:
```ini
general {
    # Enable verbose diagnostic debug logging in server/client console output.
    B:enableDebugLogging=false

    # Whether to display cooldown duration info in item hover tooltips.
    B:enableTooltips=true

    # List of item cooldown rules in the format 'modid:item[:metadata]=seconds'.
    # If metadata is omitted or '*', the cooldown applies to all variants of the item.
    S:foodCooldowns <
        minecraft:golden_apple:0=60
        minecraft:golden_apple:1=300
        minecraft:cooked_beef=30
        minecraft:bread=15
     >

    # Text prefix displayed before the cooldown duration in item tooltips.
    S:tooltipPrefix=Cooldown: 
}
```

---

## Installation & Compatibility

- **Minecraft Version**: 1.12.2
- **Required Loader**: Minecraft Forge (build 14.23.5.2847 or newer) or CleanroomMC
- **Dependencies**: None (Zero external library dependencies)
- **Multiplayer**: For optimal experience, install on both server and client. The server enforces cooldowns and timing persistence, while the client renders visual sweeps and duration tooltips.
