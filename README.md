![Minecraft](https://img.shields.io/badge/Minecraft-1.21.6%20--%201.21.8-green)
![Loader](https://img.shields.io/badge/Loader-Fabric-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Requires](https://img.shields.io/badge/Requires-Fabric%20API%20%2B%20Cloth%20Config%20API-yellow)
![Status](https://img.shields.io/badge/Status-BETA-red)

![Eroded World Banner](https://github.com/MSWHendys/Eroded_World/blob/1.21.6-1.21.8/gradle/wrapper/img/fabric%201.21.6-1.21.8.png?raw=true)

# Eroded World

**Hardcore survival overhaul for Minecraft Fabric 1.21.6 - 1.21.8**

> Darkness is not cosmetic.  
> Mining has consequences.  
> Energy defines your limits.

Eroded World turns Minecraft survival into a slower, harsher and more reactive experience. The world remembers what players do: mining scars the land, darkness becomes dangerous, overused territory grows hostile, and death leaves a trace that must be recovered.

This project is still experimental. Systems, balance values and configuration options may change between builds.

---

## Table of Contents

- [About](#about)
- [Core Systems](#core-systems)
- [Items and Blocks](#items-and-blocks)
- [Commands](#commands)
- [Configuration](#configuration)
- [Technical Information](#technical-information)
- [Wiki](#wiki)
- [License](#license)

---

## About

Eroded World is built around consequence-driven survival:

- every physical action can matter,
- mining and pollution affect local territory,
- darkness is a real threat,
- crafting quality depends on preparation and progression,
- death creates recoverable remains instead of simply deleting progress,
- server-side systems remain authoritative for survival balance.

The mod is designed for atmospheric multiplayer survival servers, especially where slow progression, risk, resource planning and long-term world impact are part of the gameplay.

---

## Core Systems

### Energy & Stamina

Energy is the player's main physical resource.

Energy can be consumed by:

- sprinting,
- dodging,
- mining,
- crafting.

Energy states include:

- **Normal** – full performance,
- **Tired** – reduced safety margin,
- **Exhausted** – sprinting becomes restricted and Mining Fatigue can be applied,
- **Empty** – the player collapses and must recover.

Energy can regenerate passively, through sleep, food and special recovery items depending on server configuration. The client HUD displays energy state, warnings and important feedback.

### 🧠 Skills / CG Progression

The mod tracks crafting growth through CG values. Current skill categories include:

- **Woodworking**
- **Smelting**

CG progression is intentionally slow and is used by crafting and quality systems to reward preparation instead of fast grinding.

### Crafting & Item Quality

Crafting is no longer just a recipe check.

The crafting system can:

- consume energy,
- grant CG experience,
- require vanilla XP level for special items,
- apply item quality,
- show feedback through client-side overlays.

Quality tiers:

- **POOR**
- **STANDARD**
- **EXCELLENT**

Quality affects item behavior through durability and repair-related modifiers. Input quality and player CG influence the resulting quality. Repairing through an anvil can degrade quality over time, making high-quality gear valuable but not permanent.

### Territory, Mining & Collapse

The world reacts to player activity.

Territory is evaluated in larger local areas based on chunk groups. These areas track:

- mining activity,
- pollution,
- forestation,
- threat.

Heavy mining increases instability. Below configured Y-levels, repeated mining can trigger cave-ins. Collapses can produce warning sounds, dust particles, falling gravel and hostile encounters.

Tunnels can be protected by nearby stabilizer blocks. The Warding Lamp can also hold back a cave-in while active.

### Dynamic Ecosystem

High-threat areas can visually decay over time:

- grass can turn into dirt,
- dirt can become coarse dirt,
- coarse dirt can become podzol,
- vegetation can be damaged in eroded areas.

If an area is left alone and threat drops, the ecosystem can slowly recover. The system is designed to make overused locations feel worn down while calmer places slowly heal.

### True Darkness

Darkness is handled as a survival mechanic, not only as a visual effect.

The system can include:

- server-side darkness detection,
- client-side dark overlay,
- eye adaptation,
- heartbeat audio,
- calm-down audio when leaving danger darkness,
- debug information for testing light levels.

Exploring caves or nights without a light source is meant to be risky.

### Mutated Mobs & Light Eater

High-threat territory can spawn special hostile mobs.

Mutated mobs can receive:

- increased health,
- custom titles such as **Forsaken**, **Eroded** or **Apocalypse**,
- special command tags,
- protection behavior against sunlight,
- light-related AI behavior.

The Light Eater system allows mutated mobs to interfere with nearby light sources, making darkness a direct part of combat pressure.

### Warding Lamp

The Warding Lamp is a special survival tool for darkness and cave exploration.

When held in dark underground conditions, it can create temporary light around the player. It has configurable duration, light level and darkness requirements. While active, it can also protect against some cave-in events.

### Death & Soul Recovery

Death is punishing, but recoverable.

When a player dies:

- their inventory is stored in a **Death Chest**,
- the death chest is protected for a configurable time,
- a hologram can appear above the remains,
- a **Return Compass** helps guide the player back,
- the compass can reveal remaining protection time and coordinates.

The system is designed to keep death meaningful without turning every death into permanent loss.

### Eroded Loot System

Naturally found containers can become part of the Eroded Loot system.

The loot system can:

- modify chest and barrel contents,
- lock protected containers until searched,
- create curated survival loot,
- include food, light sources, tools, armor, valuables and special items,
- support admin-created loot chests through commands.

### Spawn Sovereignty

Spawn can be protected as a safe preparation zone.

Depending on configuration, spawn protection can prevent:

- hostile mob pressure,
- explosions,
- block breaking,
- block placement,
- piston griefing.

Creative mode and OP bypass behavior can be configured.

---

## Items and Blocks

Current custom content includes:

- **Energy Drink** – restores energy when the player is not already full,
- **Energy Stabilizer / Adrenaline Shot** – temporarily locks energy consumption through an immunity window,
- **Return Compass** – guides players back to their latest stored death memory,
- **Death Chest** – stores player remains after death,
- **Warding Lamp** – temporary underground light and collapse protection tool,
- **Eroded Block** – decorative/custom block with multiple visual variants.

---

## Commands

Main command:

```mcfunction
/eroded
```

Available subcommands:

```mcfunction
/eroded reload
```

Reloads all Eroded World configuration files. Requires OP level 2.

```mcfunction
/eroded chest
```

Gives an admin Eroded Loot Chest item. Requires OP level 2.

```mcfunction
/eroded energy <player> <amount>
```

Sets a player's energy value. Requires OP level 2.

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

Allows players to tune the darkness heartbeat sound.

Default debug keybind:

```text
F6
```

Used for Eroded debug overlays depending on the active client systems.

---

## Configuration

Eroded World uses Cloth Config / AutoConfig.

Configuration categories include:

- `ErodedWorld/energy`
- `ErodedWorld/crafting`
- `ErodedWorld/darkness`
- `ErodedWorld/territory`
- `ErodedWorld/death`
- `ErodedWorld/combat`
- `ErodedWorld/loot`

After the first run, configuration files are generated in the Minecraft config directory. Server owners can edit JSON files manually or use compatible configuration UI tools.

Use this command after manual changes:

```mcfunction
/eroded reload
```


---

## Technical Information

- **Mod ID:** `eroded`
- **Minecraft target:** `1.21.8`
- **Minecraft compatibility range:** `1.21.6 - 1.21.8`
- **Java:** `21`
- **Loader:** Fabric
- **Architecture:** client + server split
- **Networking:** custom payloads for energy, skills, darkness, compass and feedback synchronization
- **Persistence:** PersistentState-based world and player data
- **Configuration:** Cloth Config / AutoConfig
- **Localization:** English and Czech language files are included
- **Addictions:** - Fabric API, Cloth Config API

---

## Wiki

The project wiki contains gameplay and administration documentation:

```text
https://github.com/MSWHendys/Eroded_World/wiki
```

Recommended pages:

- Getting Started
- Core Systems
- Commands
- Config Guide
- Energy & Stamina
- Darkness & Atmosphere
- Territory & Ecosystem
- Death & Soul Recovery
- Loot System

---

## License

See [`LICENSE.txt`](LICENSE.txt).

The repository currently uses an internal/custom license notice. License details may change in the future.


