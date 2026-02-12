![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Loader](https://img.shields.io/badge/Loader-Fabric-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Dependency](https://img.shields.io/badge/Requires-Fabric%20API%20%26%20Cloth%20Config%20API-yellow)

<p align="center">
  <img src="https://mcstoryworld.cz/minecraft/mods/eroded_world_banner.png" alt="Eroded World Banner" width="1000">
</p>

<h1 align="center">⛏ Eroded World</h1>
<p align="center">
Hardcore Survival Overhaul for Minecraft – Fabric 1.21.8
</p>

<p align="center">
  <strong>Darkness is not cosmetic.</strong><br>
  <strong>Mining has consequences.</strong><br>
  <strong>Energy defines your limits.</strong>
</p>

---

## 🌑 About

**Eroded World** is a survival overhaul mod for Minecraft (Fabric 1.21.8)  
that redefines the core philosophy of survival gameplay.

Mining destabilizes the environment.  
Darkness becomes a real obstacle.  
Energy limits your movement.  
Crafting depends on preparation.

The world remembers what you do.

This mod is built for immersive, slower-paced, atmosphere-driven survival.

---

## ⚒ Core Systems

### ⛏ Dynamic Territory System

- Mining increases a hidden **miningScore**
- Each 3×3 chunk area tracks player activity
- Persistent server-side world memory
- Environmental instability scales with mining intensity

The more aggressively you exploit the world, the more unstable it becomes.

---

### 💥 Environmental Collapse System

- Event-based cave-in mechanics
- Activated after miningScore reaches a threshold
- Per-cell cooldown system
- Structural reinforcement using:
    - `OAK_LOG`
    - `STRIPPED_OAK_LOG`
- Possible ambient mob spawn (no forced combat)

Caves are no longer guaranteed safe spaces.  
Structural planning matters.

---

### ⚡ Energy & Stamina System

Energy is a central survival resource in Eroded World.

- Sprint consumes energy
- Double-tap **W** dodge consumes energy
- Crafting consumes energy
- Low energy restricts movement and actions
- Server-authoritative validation
- No infinite sprint or dodge spamming

Energy regenerates through food consumption.

Reckless movement leads to exhaustion.  
Exhaustion leads to vulnerability.

---

### 🏃 Double-W Dodge Mechanic

- Double-tap **W** to perform a directional dodge
- Server-authoritative validation
- Energy cost per use
- Designed for tactical repositioning
- Cannot be spammed infinitely

Movement is no longer automatic — it is intentional.

---

### 🛠 Crafting Energy & Quality System

Crafting is no longer a passive action.

- Crafting consumes energy
- Energy level directly influences crafting performance
- Certain items (tools, equipment, selected items) are affected by quality

Crafted items can have one of three quality tiers:

- **POOR** – reduced durability or effectiveness
- **NORMAL** – standard vanilla-equivalent quality
- **EXCELLENT** – increased durability or enhanced properties

Crafting while exhausted increases the probability of poor results.  
Preparation increases the chance of excellence.

Energy, movement, and crafting are interconnected systems.

---

### 🌘 True Darkness System

- Server + client synchronized
- Non-cosmetic darkness
- Prevents simple brightness bypass
- Reinforces underground tension

Darkness is part of the survival challenge — not just an effect.

---

## 🧠 Design Philosophy

Eroded World is not about grinding.  
It is about decision-making.

- When to mine
- When to reinforce
- When to retreat
- When to conserve energy
- When to craft

Every system interacts.  
Every action has consequences.

---

## 🧩 Dependencies

Required libraries:

- **Fabric API**
- **Cloth Config API**

Ensure all dependencies are installed before launching.

---

## 🛠 Technical Information

- **Minecraft:** 1.21.8
- **Loader:** Fabric
- **Java:** 21
- **Required APIs:** Fabric API, Cloth Config API
- **Architecture:** Server + Client split
- **PersistentState-based chunk tracking**
- **Event-driven environmental mechanics**
- **Server-authoritative movement & energy systems**

---

## 📦 Installation

1. Install **Fabric Loader 1.21.8**
2. Install **Fabric API**
3. Install **Cloth Config API**
4. Place `eroded_world.jar` into your `/mods` folder
5. Launch Minecraft

---

## 🚧 Development Status

✔ Dynamic Territory System  
✔ Environmental Collapse System  
✔ Energy & Stamina System  
✔ Double-W Dodge Mechanic  
✔ True Darkness System  
✔ Crafting Energy Integration  
🚧 Interdependent Crafting System (in development)  
🚧 Extended Craft Quality System (planned)

---

## 🎮 Intended For

- Hardcore survival servers
- Immersive modpacks
- Roleplay worlds
- Atmosphere-focused gameplay
- Slow-progression survival experiences

---

## 🧩 MCSTORYWORLD

Eroded World is the core system behind:

**MCSTORYWORLD – Eroded Survival**

A custom Fabric server focused on:

- environmental pressure
- structural mining consequences
- energy-based movement
- darkness-driven exploration
- meaningful survival decisions

---

## 📜 License

Currently under internal / custom license.  
License details may change in the future.

---

## 🤝 Contributing

This project is under active development.  
Feedback, bug reports, and technical suggestions are welcome.

---

## ⚠ Disclaimer

Eroded World intentionally modifies core survival mechanics.

It is designed for players who want tension, atmosphere, and consequence-driven gameplay — not casual progression.

---

<p align="center">
  ⛏ Survive carefully.
</p>
