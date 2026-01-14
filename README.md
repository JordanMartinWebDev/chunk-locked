# Chunk Locked

A Minecraft Fabric mod that transforms gameplay into a progression-based chunk unlocking experience. Start small, expand strategically.

## Overview

**Chunk Locked** restricts players to a limited playable area that expands as they progress through the game. Players begin with a 2x2 chunk starting area and unlock additional chunks by completing Minecraft advancements. Each advancement grants unlock credits, which players spend using commands to unlock adjacent chunks.

### 🎮 Credit System

**Current Implementation**: Each advancement grants 1 credit (configurable)

- Configurable per-advancement rewards via JSON config
- Advancement blacklist support
- Hot-reload with `/reload` command

**Future**: Game mode selection (Easy/Extreme) with scaled rewards planned

## Core Features

### 🗺️ Chunk-Based Progression System

- **Starting Area**: 2x2 chunks (32x32 blocks) automatically unlocked on first join
- **Advancement Unlocking**: Complete advancements to earn unlock credits (configurable per advancement)
- **Command-Based Unlocking**: Use `/chunklocked unlockfacing` to unlock the chunk you're looking at
- **Barrier System**: Physical barrier blocks at chunk boundaries prevent access to locked chunks
- **Entity-Specific Collision**: Barriers block players but allow mobs, items, and projectiles to pass through
- **Access Control**: Per-player chunk tracking with server-side validation

### 🌍 Multi-Dimensional Mechanics

- **Overworld**: Physical barrier blocks enforce chunk boundaries
- **Nether & End**: No chunk restrictions for free exploration
- **Nether Portal Auto-Unlock**: Exiting a Nether portal in a locked chunk automatically unlocks it (if credits available)
- **Locked Chunk Penalty**: Wither II effect applied when in locked chunks with no credits
  - Continuous reapplication prevents milk bucket escape
  - Automatically removed when leaving locked area or entering Nether/End
  - Warning messages guide players back to safety

## Technical Information

- **Minecraft Version**: 1.21.11
- **Mod Loader**: Fabric 0.18.4
- **Java Version**: 21
- **Fabric API**: 0.141.1+1.21.11

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download the latest release of Chunk Locked
4. Place both mod files in your `.minecraft/mods` folder
5. Launch Minecraft with the Fabric profile

## Building from Source

```bash
# Clone the repository
git clone https://github.com/JordanMartinWebDev/chunk-locked.git
cd chunk-locked

# Build the mod
./gradlew build

# Output: build/libs/chunk-locked-1.0.0.jar
```

## Development Status

**Current Version**: 0.1.0-alpha (In Development)  
**Playable**: ✅ Yes - Core mechanics functional, UI in development

---

### 🎯 User-Facing Features

These are features that are **complete and usable** by players in-game:

#### ✅ Advancement Credit System (WORKING - 100% Complete)

- [x] Earn credits per advancement completed
- [x] Credits persist across game sessions
- [x] In-game notifications when credits are earned
- [x] View credit balance with `/chunklocked credits`
- [x] Credits displayed in action bar after advancement completion

#### ✅ Initial Spawn Setup (WORKING - 100% Complete)

- [x] 2x2 chunks automatically unlocked on first join
- [x] Barriers placed around starting area
- [x] Welcome message displayed to new players

#### ✅ Barrier Block System (WORKING - 100% Complete)

- [x] Physical barriers at chunk boundaries
- [x] Barriers only between locked/unlocked chunks (optimized)
- [x] Entity-specific collision (players blocked, mobs pass through)
- [x] Ender pearl blocking (prevents teleporting through barriers)
- [x] Block break protection (barriers can't be destroyed)
- [x] Auto-update when chunks unlock
- [x] Full world height coverage (Y=-64 to Y=319)

#### ✅ Nether Portal Auto-Unlock (WORKING - 100% Complete)

- [x] Exit a Nether portal directly into an unlocked chunk
- [x] Auto-unlock the chunk you arrive in (if you have credits)
- [x] Instantly removes barriers around newly unlocked chunks
- [x] Displays success message with remaining credits
- [x] Can use Nether to reach distant chunks efficiently

#### ✅ Locked Chunk Penalties (WORKING - 100% Complete)

- [x] **Wither II effect** applied to players in locked chunks with no credits
- [x] Continuous effect reapplication (can't use milk buckets to escape)
- [x] Warning messages remind players to return to unlocked areas
- [x] Effect instantly disappears when leaving locked chunk
- [x] Effect disappears when entering Nether/End (safe zones)
- [x] Encourages strategic chunk unlocking and planning

#### ✅ Admin Commands (WORKING - 100% Complete)

- [x] `/chunklocked givecredits <amount>` - Give credits to self
- [x] `/chunklocked givecredits <player> <amount>` - Give credits to player
- [x] `/chunklocked unlock <x> <z>` - Unlock specific chunk
- [x] `/chunklocked unlockfacing` - Unlock chunk in facing direction (raycasting)
- [x] `/chunklocked credits` - Check credit balance
- [x] `/chunklocked debugbarriers` - View frontier and barrier info
- [x] `/chunklocked help` - Show all commands

#### ✅ Configuration System (Usable)

- [x] JSON configuration file (`config/chunk-locked-advancements.json`)
- [x] Custom reward amounts per advancement
- [x] Advancement blacklist support
- [x] Hot-reload configuration with `/reload` command

#### 🚧 Chunk Progression System (Usable via Commands)

- ✅ Chunk unlock tracking and persistence
- ✅ Command-based chunk unlocking (`/chunklocked unlockfacing`)
- ✅ Raycasting system for intelligent chunk selection
- ❌ Interactive chunk map UI (planned)
- ❌ Adjacency enforcement (currently can unlock any chunk)
- ❌ In-game credit display HUD (using commands instead)

#### 🚧 Biome Discovery System (Planned)

- [ ] Map UI biome layer overlay
- [ ] Biome-based exploration rewards
- [ ] Integration with "Adventuring Time" advancement

---

### 📊 Completion Status

| Phase                           | Status         | Completion |
| ------------------------------- | -------------- | ---------- |
| Phase 1: Core Systems           | ✅ COMPLETE    | 100%       |
| Phase 2: Dimensional Mechanics  | ✅ COMPLETE    | 100%       |
| Phase 3: UI & Visualization     | ❌ NOT STARTED | 0%         |
| Phase 4: Gameplay Polish        | ⚠️ PARTIAL     | 30%        |
| Phase 5: Testing & Optimization | ⚠️ PARTIAL     | 70%        |

**Overall Project**: ~75% code complete, **80% playable** (missing UI, using commands instead)

---

## Documentation

- **[PROJECT_STATUS.md](PROJECT_STATUS.md)** - Detailed feature completion checklist
- **[Documentation/](Documentation/)** - API documentation for implemented systems
  - [Architecture.md](Documentation/Architecture.md) - System architecture overview
  - [AdvancementSystem.md](Documentation/AdvancementSystem.md) - Credit system API
  - [CommandsAPI.md](Documentation/CommandsAPI.md) - Admin command reference
- **[manual-tests/](manual-tests/)** - Manual test cases for verification
- **[docs/00-overview.md](docs/00-overview.md)** - High-level project overview

---

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Author

**JordanMartinWebDev**

---

_Built with ❤️ using Fabric_
