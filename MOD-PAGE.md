<span style="color:#d6a100">**AI usage disclaimer:** This mod was developed with AI-agent assistance using [this agent workflow](https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow). The project owner reviewed the work during development.</span>

Adds server-enforced consumption cooldowns, with duration tooltips and cooldown overlays in the hotbar and inventory.

By default, only standard Golden Apples and Enchanted Golden Apples receive cooldowns of 1 minute and 5 minutes. Server owners can instead apply one fixed cooldown to every standard food, or scale each food's cooldown by the amount of hunger it restores. Per-item rules become overrides in either all-food mode, and a zero-second rule can exclude an item.

Required on both client and server.

[View the source on GitHub](https://github.com/mahghuuuls/food-timer).

## Configuration

The configuration file is `config/foodtimer.cfg`. Changes require a restart.

Choose one `cooldownPolicy`:

- `CONFIGURED_ONLY`: Only matching `foodCooldowns` entries receive cooldowns. This is the default and preserves the original behavior.
- `FIXED_ALL_FOODS`: Every standard food uses `fixedFoodCooldownSeconds`, unless a `foodCooldowns` rule overrides it.
- `SCALED_ALL_FOODS`: Every standard food uses restored hunger points multiplied by `secondsPerHungerPoint`, unless a `foodCooldowns` rule overrides it.

Rules use `modid:item[:metadata]=seconds`. Exact metadata rules win over wildcard rules. Omit metadata or use `*` to match every variant. Use `0` seconds to exclude a matching item from cooldowns.

```ini
general {
    S:cooldownPolicy=CONFIGURED_ONLY
    I:fixedFoodCooldownSeconds=30
    I:secondsPerHungerPoint=5

    S:foodCooldowns <
        minecraft:golden_apple:0=60
        minecraft:golden_apple:1=300
     >

    B:enableTooltips=true
    S:tooltipPrefix=Cooldown: 
    B:enableDebugLogging=false
}
```

Examples:

```ini
# Override every metadata variant of bread with 20 seconds.
minecraft:bread=20

# Give normal Golden Apples 60 seconds but exclude Enchanted Golden Apples.
minecraft:golden_apple:0=60
minecraft:golden_apple:1=0
```

The server controls gameplay cooldowns and sends its active policy to connected clients. Each client still controls whether tooltips are visible and what tooltip prefix is displayed.
