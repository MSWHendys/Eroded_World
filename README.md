![Minecraft](https://img.shields.io/badge/Minecraft-1.21.8-green)
![Loader](https://img.shields.io/badge/Loader-Fabric-blue)
![Java](https://img.shields.io/badge/Java-21-orange)
![Dependency](https://img.shields.io/badge/Requires-Fabric%20API%20%26%20Cloth%20Config%20API-yellow)
![Status](https://img.shields.io/badge/Status-ALPHA-red)

<p align="center">
  <img src="https://mcstoryworld.cz/minecraft/mods/eroded_world_banner.png" alt="Eroded World Banner" width="1000">
</p>

<h1 align="center">⛏ Eroded World</h1>
<p align="center">
Hardcore Survival Overhaul for Minecraft – Fabric 1.21.8
</p>

<p align="center">
  <strong>⚠️ WARNING: This mod is currently in ALPHA stage. Features may change and bugs may occur.</strong>
</p>

<p align="center">
  <strong>Darkness is not cosmetic.</strong><br>
  <strong>Mining has consequences.</strong><br>
  <strong>Energy defines your limits.</strong>
</p>

---

## 🌑 About

**Eroded World** is a survival overhaul mod for Minecraft (Fabric 1.21.8) that redefines the core philosophy of survival gameplay.

The world remembers what you do. This mod is built for immersive, slower-paced, atmosphere-driven survival where every action has a lasting impact.

---

## ⚒ Core Systems

### ⛏ Dynamic Territory & Ecosystem
- **Territory Tracking:** Each 3×3 chunk area tracks mining, pollution, and forestation.
- **Environmental Decay:** High pollution and mining cause grass to turn into dirt, then coarse dirt, and eventually podzol. Leaves can wither in highly eroded zones.
- **Natural Regrowth:** If an area is left undisturbed and pollution decreases, the ecosystem will slowly heal and grass will return.

---

### 💥 Environmental Collapse System
- **Cave-ins:** Mining below Y=50 can trigger structural collapses once the miningScore is too high.
- **Warning Signs:** Players hear the rock creaking and see dust particles before a collapse.
- **Reinforcement:** Structural planning matters. Use logs or "Stabilizers" to secure tunnels and prevent cave-ins.

---

### ⚡ Energy & Stamina System
- **Actions:** Sprinting, dodging, mining, and crafting all consume energy.
- **Exhaustion:** Low energy levels apply **Mining Fatigue** and restrict sprinting.
- **Dynamic HUD:** A custom HUD displays your energy state (Normal, Tired, Exhausted, Empty).

---

### 💀 Death Persistence & Soul Recovery
- **Death Chest (Remains):** Upon death, a special "Death Ender Chest" spawns, containing your items.
- **Magical Protection:** Your remains are protected by a timed magical seal, preventing other players from looting them for a configurable duration.
- **Dynamic Holograms:** A floating 3D hologram of your head and a countdown timer appear above your remains.
- **Return Compass:** You respawn with a "Return Compass" that points to your last death location.

---

### 🛠 Crafting & Item Quality
- **Quality Tiers:** Items can be **POOR**, **STANDARD**, or **EXCELLENT**.
- **Lore Integration:** Tooltips display durability and repair multipliers based on quality.
- **Anvil Degradation:** Repairing items gradually lowers their quality tier. Preparation increases the chance of excellence.

---

### 👾 Mutated Entities & The Light Eater
- **Mutated Mobs:** In high-threat areas, monsters spawn with increased health, resistance, and unique titles (Forsaken, Eroded, Apocalypse).
- **Light Fear AI:** Hostile mobs actively avoid bright light sources in dark environments.
- **Light Eater:** Mutated monsters will actively try to extinguish your torches, lanterns, and campfires to plunge you back into darkness.

---

### 🛡 Spawn Sovereignty
- **Safe Zone:** A configurable radius around the world spawn provides invulnerability to players.
- **Anti-Grief:** Explosions, block breaking, and piston movement are disabled within the spawn territory for non-admin players.
- **Repulsion:** Hostile monsters are physically pushed out of the protected spawn area.

---

## 📜 Commands

- `/eroded reload` – Reloads all configuration files (OP level 2 required).
- `/eroded chest` – Gives the player a special Eroded Loot Chest item (OP level 2 required).
- `/eroded icon <position>` – Changes the position of the Energy HUD (e.g., `left_down`, `center_up`).
- `/eroded sound volume <1-10>` – Adjusts the heartbeat sound volume.
- `/eroded sound delay <1-10>` – Adjusts the interval between heartbeats.
- `/eroded sound info` – Displays your current sound tuning settings.
- `/eroded sound reset` – Resets sound settings to default.

---

## 🧩 Dependencies
Required libraries:
- **Fabric API**
- **Cloth Config API**

---

## 🛠 Technical Information
- **Minecraft:** 1.21.8 / **Java:** 21
- **Architecture:** Server + Client split.
- **Data:** Uses **PersistentState API** for stable chunk and player data tracking.
- **Localization:** Full support for 🇨🇿 Czech and 🇺🇸 English.

---

<p align="center">
  ⛏ <strong>Survive carefully. This is still an ALPHA.</strong>
</p>