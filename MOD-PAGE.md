<p style="color: #d6a100;"><strong>AI usage disclaimer:</strong> This mod was developed with AI-agent assistance using <a href="https://github.com/mahghuuuls/minecraft-1.12.2-mod-agent-workflow">this exact agent workflow</a>. The project owner reviewed the work during development.</p>

Adds configurable consumption cooldowns and duration tooltips to foods and consumables, preventing food spamming. By default it adds a cooldown for Golden Apples (1 min) and Enchanted Golden Apples (5 min). Any food can be added in the config with any duration, it also adds tooltips showing the exact cooldown duration (e.g. `Cooldown: 1m` or `Cooldown: 5m`).

- Install on both server and client.

## Configuration

The configuration file is located at `config/foodtimer.cfg`.

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