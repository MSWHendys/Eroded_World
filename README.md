![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10-green)
![Loader](https://img.shields.io/badge/Loader-Fabric-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Requires](https://img.shields.io/badge/Requires-Fabric%20API-yellow)
![Status](https://img.shields.io/badge/Status-Release-green)

![Eroded World Banner](https://github.com/MSWHendys/Eroded_World/blob/1.21.10/gradle/wrapper/img/fabric%201.21.10.webp?raw=true)

# Eroded World

**Hardcore survival overhaul for Minecraft Fabric 1.21.10**

> Darkness is not cosmetic.  
> Mining has consequences.  
> Energy defines your limits.

Eroded World turns ordinary survival actions into long-term decisions. Mining, movement, crafting, darkness, territory pressure, death recovery and player protection are connected into one harsher survival loop.

Current project version: **`1.1.0_1.21.10`**

---

## Table of Contents

- [About](#about)
- [Current Release Highlights](#current-release-highlights)
- [Installation](#installation)
- [Core Systems](#core-systems)
- [Items and Blocks](#items-and-blocks)
- [Recipes](#recipes)
- [Commands](#commands)
- [Configuration](#configuration)
- [Technical Information](#technical-information)
- [Wiki](#wiki)
- [License](#license)

---

## About

Eroded World is built around consequence-driven survival:

- physical actions consume Energy,
- mining affects both the player and the surrounding territory,
- heavily exploited areas can become unstable and hostile,
- darkness is a gameplay threat rather than only a visual effect,
- crafting progression influences quality and efficiency,
- death creates recoverable remains and a Return Compass,
- player bases and world spawn can be protected by configurable server-side rules,
- important gameplay state is kept authoritative on the server and synchronized to clients.

The mod is intended for atmospheric survival and multiplayer servers where slower progression, resource planning and long-term world impact are part of the experience.

---

## Current Release Highlights

This release includes the current reworked survival systems and several important reliability improvements:

- removed the Cloth Config / AutoConfig dependency,
- added built-in JSON configuration loading, validation, migration and recovery,
- missing configuration keys are automatically added from current defaults while existing values are preserved,
- invalid configuration files are backed up and restored to the last known valid state,
- mining Energy costs are separated into **hard** and **soft** block profiles,
- mining speed now scales with remaining Energy,
- territory claims support multiple anchors, connected claims and per-player access permissions,
- spawn protection covers more forms of griefing and hostile pressure,
- cave collapse, ecosystem decay and mutated mob systems are integrated with territory activity,
- Death Chests, Return Compass behavior and temporary post-death protection are integrated into the recovery loop,
- Eroded recipes include recipe-book unlock advancements,
- client/server synchronization and persistent state handling are used across Energy, skills, territory and death systems.

---

## Installation

### Requirements

- Minecraft `1.21.10`
- Fabric Loader `0.17.2+`
- Fabric API `0.138.4+`
- Java `21`

**Cloth Config API is not required.**

### Client / Server

Eroded World contains both server and client systems and should be installed on:

- the server,
- all connecting clients.

### Setup

1. Install Fabric Loader for a supported Minecraft version.
2. Install Fabric API.
3. Place the Eroded World `.jar` file into the `mods` folder.
4. Start the game or server once to generate configuration files.
5. Edit the generated JSON configuration files if needed.
6. Use `/eroded reload` after manual configuration changes.

---

## Core Systems

### Energy, Stamina & Mining

Energy is the player's main physical resource and is used by several systems.

Energy can be consumed by:

- sprinting,
- dodging,
- mining,
- crafting.

Energy can recover through passive regeneration, sleep, food and special recovery items depending on configuration.

Mining uses separate profiles for **hard** and **soft** blocks. Vanilla mining tags are used to classify blocks and determine whether the held tool is fast, slow, natural hand-work or genuinely unsuitable.

Default mining speed scaling:

- **51-100% Energy** – normal mining speed,
- **21-50% Energy** – 50% mining speed,
- **0-20% Energy** – 10% mining speed.

The zero-Energy behavior, mining costs and tool multipliers are configurable. Hard and soft blocks can use different `blocksPerEnergy`, `slowToolMultiplier` and `wrongToolMultiplier` values.

The Energy HUD can be moved to several screen positions and displays warnings for tired, exhausted and empty Energy states.

### Combat & Movement

Sprint and dodge behavior can consume Energy.

The combat configuration controls:

- sprint drain interval,
- Energy cost while sprinting,
- minimum Energy required to sprint,
- dodge Energy cost,
- dodge cooldown,
- dodge distance and direction rules.

### Skills / CG Progression

The mod tracks crafting growth through CG values.

Current skill categories include:

- **Woodworking**
- **Smelting**

CG progression is used by crafting and quality systems to reward continued progression instead of instant access to the best results.

### Crafting & Item Quality

Crafting is connected to Energy, recipe difficulty and skill progression.

The crafting system can:

- consume Energy,
- grant CG experience,
- use recipe difficulty multipliers,
- apply item quality,
- use input quality when calculating output quality,
- display crafting feedback to the player.

Quality tiers:

- **POOR**
- **STANDARD**
- **EXCELLENT**

Quality can affect durability and repair behavior. Repairing items through an anvil can reduce their quality over time.

### Territory, Mining Pressure & Cave Collapse

Environmental territory is separate from player claims. Territory cells track activity such as:

- mining,
- pollution,
- forestation,
- threat.

Heavy mining can raise local danger. Below the configured Y level, unstable territory can trigger cave collapses with warning effects, falling material and a chance of hostile encounters.

Tunnel stabilizers use the `eroded:stabilizers` block tag. In the bundled data pack, Minecraft logs are valid stabilizer blocks. A stabilizer must be installed above the tunnel area to protect against collapses. An active Warding Lamp can also hold back a collapse.

Old inactive territory state can be pruned automatically to reduce unnecessary long-term world data.

### Dynamic Ecosystem

High-threat areas can visibly degrade over time.

The ecosystem can transition terrain through states such as:

- grass -> dirt,
- dirt -> coarse dirt,
- coarse dirt -> podzol or dirt,
- leaf loss in damaged areas.

When territory becomes calm enough, dirt and coarse dirt can recover back to grass.

### Mutated Mobs

Threatened territory can spawn Eroded hostile variants.

The project includes custom:

- **Eroded Skeleton**
- **Eroded Zombie**

Mutated mobs can receive increased health, custom threat titles and special sunlight/light behavior. Spawn frequency, surface-only spawning, health limits, despawn distance and related behavior are configurable.

### True Darkness & Light Eater

Darkness is handled as a survival mechanic rather than only a visual filter.

The system includes:

- server-side darkness evaluation,
- client-side darkness overlay and eye adaptation,
- heartbeat audio,
- calm-down audio after leaving dangerous darkness,
- light-sensitive hostile AI,
- Light Eater behavior in sufficiently dangerous territory.

The Light Eater can interfere with nearby vanilla torches, soul torches, lanterns, soul lanterns, campfires and soul campfires.

### Warding Lamp

The **Warding Lamp** is a special underground survival tool.

When active under suitable darkness conditions, it can provide temporary light around the player. Its duration, light level, skylight limit and underground height are configurable.

An active Warding Lamp can also prevent a cave-collapse event.

### Eroded Torch

The **Eroded Torch** is a rechargeable handheld and placeable light source.

Depending on configuration, it can:

- provide dynamic light while held,
- use a limited charge,
- recharge while inactive,
- drain charge only in darkness,
- work as a placeable torch with configurable light level.

### Territory Protection

Player territory protection uses two custom items:

- **Territory Anchor**
- **Territory Module**

A Territory Module is inserted into an anchor and the claim becomes active after a configurable stabilization delay.

The claim system supports:

- multiple claims per player,
- configurable claim radius,
- overlap prevention,
- connected claims owned by the same player,
- trusted-player access,
- separate permissions for building, breaking, containers, redstone, fire and entities,
- protection against explosions, fluids, automation transfers, projectiles, vehicles and mob griefing.

A protected claim is base protection, not a complete safety bubble. Darkness, Energy exhaustion, cave collapses and other survival mechanics can still affect the player.

### Spawn Protection

World spawn has its own configurable protection system.

It can protect the area from:

- hostile mob pressure,
- player damage inside spawn,
- explosions,
- block breaking and placement,
- piston interaction,
- fluid flow,
- inventory automation across boundaries,
- dispenser/dropper boundary actions,
- projectiles,
- mob griefing,
- vehicle damage and interaction,
- selected protected entity interactions.

Creative and OP bypass behavior can be configured separately.

### Death Chest & Soul Recovery

Death is punishing, but recoverable.

When a player dies, the system can:

- store inventory in a **Death Chest**,
- protect the remains for a configurable duration,
- show a hologram above the remains,
- store death memory persistently,
- provide a **Return Compass** that points toward the latest stored death location,
- show death coordinates and remaining recovery time,
- temporarily protect the player after respawn.

Post-death protection can prevent incoming damage, clear hostile mob targets and optionally end when the player attacks.

### Return Compass

The **Return Compass** is both a recovery tool and an emergency weapon.

It can:

- guide the player toward the latest death memory,
- display recovery information,
- briefly weaken dangerous darkness through its darkness-break ability,
- damage configured living targets while a valid death memory exists.

Damage, cooldown and target rules are configurable.

### Eroded Loot System

Naturally found containers can participate in the Eroded Loot system.

The system can:

- replace or modify eligible container loot,
- protect Eroded loot containers,
- generate a configurable number of items,
- use configurable per-item chances and stack sizes,
- include vanilla survival supplies and Eroded World items,
- create admin loot chests through `/eroded chest`.

---

## Items and Blocks

Current custom content includes:

- **Energy Drink** – restores Energy,
- **Energy Stabilizer** (`eroded:adrenaline_shot`) – temporarily prevents Energy consumption,
- **Return Compass** – guides players back to their latest death memory and can act as an emergency weapon,
- **Death Chest** – stores player remains after death,
- **Warding Lamp** – temporary underground light and collapse-protection tool,
- **Eroded Torch** – rechargeable handheld and placeable light source,
- **Territory Anchor** – core block for player territory protection,
- **Territory Module** – activates and manages Territory Anchors,
- **Eroded Block** – custom decorative/building block,
- **Eroded Skeleton Spawn Egg**,
- **Eroded Zombie Spawn Egg**.

---

## Recipes

Bundled crafting recipes are included for:

- Energy Drink,
- Energy Stabilizer,
- Eroded Block,
- Warding Lamp,
- Eroded Torch,
- Territory Anchor,
- Territory Module.

Each Eroded recipe includes a recipe-book advancement and unlocks when the player obtains at least one ingredient associated with that recipe.

---

## Commands

Main command:

```mcfunction
/eroded
```

### Administration commands

Requires permission level 2 unless stated otherwise.

```mcfunction
/eroded reload
```

Reloads and validates all Eroded World configuration files.

```mcfunction
/eroded chest
```

Gives the executing player an admin Eroded Loot Chest item.

```mcfunction
/eroded energy <player> <amount>
```

Sets a player's Energy value.

```mcfunction
/eroded territory info
```

Shows information about the claim at the current position.

```mcfunction
/eroded territory list
```

Lists claims in the current dimension.

```mcfunction
/eroded territory remove
/eroded territory remove <pos>
```

Removes the claim at the current or specified position and removes its anchor block.

```mcfunction
/eroded territory cell
```

Shows environmental territory-cell information for the player's current location.

```mcfunction
/eroded territory cells
```

Shows the number of stored environmental territory cells in the current dimension.

```mcfunction
/eroded territory connected
```

Shows which claims owned by the current claim owner are connected to the claim at the player's position.

```mcfunction
/eroded territory clear
```

Removes all player claims in the current dimension and removes their anchor blocks.

### Player commands

```mcfunction
/eroded icon <position>
```

Changes the Energy HUD position.

Supported positions:

- `left_down`
- `right_down`
- `center_down`
- `left_up`
- `right_up`
- `center_up`

```mcfunction
/eroded sound volume <1-10>
/eroded sound delay <1-10>
/eroded sound info
/eroded sound reset
```

Allows the player to tune darkness heartbeat audio.

### Debug keybind

Default key:

```text
F6
```

Toggles Eroded debug overlays when the relevant client debug systems are available.

---

## Configuration

Eroded World uses its own JSON configuration system. Cloth Config and AutoConfig are not used.

Configuration directory:

```text
config/ErodedWorld/
```

Generated files:

```text
combat.json
crafting.json
darkness.json
death.json
energy.json
territory.json
loot.json
```

### Safe configuration loading

Configuration files are strictly parsed and validated before becoming active.

The loader can:

- reject malformed JSON and invalid values,
- validate numeric ranges and required objects,
- merge newly introduced keys from current defaults,
- preserve existing administrator values,
- preserve unknown legacy keys where possible,
- keep a last-known-good snapshot in `config/ErodedWorld/.last-good/`,
- copy invalid files into `config/ErodedWorld/.invalid/`,
- restore a previous valid configuration after a failed reload.

A normal `/eroded reload` behaves transactionally: if one configuration is invalid, the new configuration set is not partially activated.

After manual changes, use:

```mcfunction
/eroded reload
```

---

## Technical Information

- **Mod ID:** `eroded`
- **Version:** `1.1.0_1.21.10`
- **Minecraft target:** `1.21.10`
- **Minecraft compatibility range:** `1.21.10`
- **Java:** `21`
- **Loader:** Fabric `0.17.2+`
- **Fabric API:** `0.138.4+`
- **Environment:** client + server
- **Mappings:** Mojang official mappings
- **Networking:** custom Fabric payloads for Energy, skills, darkness, compass, territory and UI synchronization
- **Persistence:** Minecraft `PersistentState`-based world/player data and custom death/territory storage
- **Configuration:** built-in strict JSON configuration system
- **Localization:** English and Czech language files
- **Required runtime dependencies:** Fabric Loader, Fabric API, Java 21

---

## Wiki

Project documentation:

https://github.com/MSWHendys/Eroded_World/wiki

Useful topics include installation, commands, configuration, Energy, darkness, territory, death recovery, loot and special items.

---

## License

The project metadata declares the mod license as **MIT**.
