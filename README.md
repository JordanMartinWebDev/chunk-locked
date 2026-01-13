# Chunk Locked

A Minecraft Fabric mod that transforms gameplay into a progression-based chunk unlocking experience. Start small, expand strategically.

## Overview

**Chunk Locked** restricts players to a limited playable area that expands as they progress through the game. Players begin with a 2x2 chunk starting area and unlock additional chunks by completing Minecraft advancements. Each advancement grants unlock credits based on its difficulty and the selected game mode, which players strategically spend to unlock chunks through a custom UI map.

### 🎮 Game Modes

**Chunk Locked: Easy** - Casual exploration experience

- Task advancements: 1-2 credits
- Goal advancements: 3-5 credits
- Challenge advancements: 10-15 credits
- ~250-300 total credits available

**Chunk Locked: Extreme** - Hardcore challenge

- Task advancements: 1 credit
- Goal advancements: 1 credit
- Challenge advancements: 5 credits
- ~120-150 total credits available

## Core Features

### 🗺️ Chunk-Based Progression System

- **Starting Area**: 2x2 chunks (32x32 blocks) as your initial playable space
- **Game Mode Selection**: Choose Easy or Extreme mode during world creation
- **Advancement Unlocking**: Complete advancements to earn unlock credits (amount based on difficulty)
- **Strategic Selection**: Choose which adjacent chunks to unlock via an interactive map UI
- **Biome Discovery**: Map overlay shows biome types to plan exploration routes
- **Access Control**: Custom chunk restriction system (not using native WorldBorder due to limitations)
- **Visual Boundaries**: Gray haze shader and particle effects for locked chunks

### 🌍 Multi-Dimensional Mechanics

- **Overworld**: Chunk access restrictions with custom enforcement system
- **Nether & End**: No chunk restrictions for free exploration
- **Nether Portal Linking**: Special mechanics for cross-dimensional travel
  - Exiting a Nether portal in a locked chunk automatically unlocks it (if credits available)
  - Creates disconnected playable areas when unlocking distant chunks
  - Poison damage penalty when exiting without available unlock credits

### 🗺️ Interactive Map Interface

- Visual representation of unlocked vs locked chunks
- Display all disconnected playable areas
- Chunk selection interface for spending unlock credits
- Real-time chunk status updates

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
**Playable**: ❌ Not Yet - Core mechanics under construction

---

### 🎯 User-Facing Features

These are features that are **complete and usable** by players in-game:

#### ✅ Advancement Credit System (WORKING - 100% Complete)

- [x] Earn credits per advancement completed
- [x] Credits persist across game sessions
- [x] In-game notifications when credits are earned
- [x] View credit balance with `/chunklocked credits`
- [x] Credits displayed in action bar after advancement completion

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

#### ⚠️ Barrier Blocks (WORKING - 100% Complete)

- [x] Physical barrier blocks at chunk boundaries
- [x] Barriers only between locked and unlocked chunks
- [x] Auto-update when chunks are unlocked
- [x] Prevents building holes in barriers
- [x] Invisible to players (only blocking)

#### ⚠️ Admin Commands (WORKING - 100% Complete)

- [x] `/chunklocked givecredits <amount>` - Give credits to self
- [x] `/chunklocked givecredits <player> <amount>` - Give credits to player
- [x] `/chunklocked unlock <x> <z>` - Unlock specific chunk
- [x] `/chunklocked unlockfacing` - Unlock chunk in facing direction
- [x] `/chunklocked credits` - Check credit balance
- [x] `/chunklocked debugbarriers` - View frontier and barrier info

#### ✅ Configuration System (Usable)

- [x] JSON configuration file (`config/chunk-locked-advancements.json`)
- [x] Custom reward amounts per advancement
- [x] Advancement blacklist support
- [x] Hot-reload configuration with `/reload` command

#### 🚧 Chunk Progression System (In Development - Not Usable)

- [ ] 🔴 **BLOCKING**: Chunk access control system (not implemented - custom mixins needed)
- [ ] 🔴 **BLOCKING**: Initial 2x2 spawn area (not implemented)
- [ ] 🔴 **BLOCKING**: Chunk selection interface (not implemented)
- [ ] 🔴 **BLOCKING**: Admin commands for testing (not implemented)

#### 🚧 Biome Discovery System (Planned)

- [ ] Map UI biome layer overlay (free - shows all biomes)
- [ ] Cartographer villager biome maps (expensive - locates specific biomes)
- [ ] Colorblind-friendly biome color palette
- [ ] Makes "Adventuring Time" advancement achievable

> **Note**: While the internal systems track chunks and credits, there is currently no way for players to spend credits or enforce chunk restrictions. The mod loads but provides no gameplay restrictions yet. Research determined that custom chunk access control (via mixins) is required instead of native WorldBorder (see docs/core-systems/world-border/DECISION.md).

---

### 🛠️ Development Progress (Internal Systems)

These systems are implemented **code-wise** but may not be exposed to users yet:

#### ✅ Backend Systems (Complete)

- [x] Chunk unlock tracking data structures
- [x] Advancement event listening
- [x] Credit management logic
- [x] NBT data persistence (save/load)
- [x] Network synchronization (server ↔ client)
- [x] Multi-area detection algorithm
- [x] Configuration management

#### 🚧 Backend Systems (In Progress)

- [ ] Game mode system (Easy/Extreme selection)
- [ ] Frame-type-based credit calculation
- [ ] Advancement frame detection (Task/Goal/Challenge)
- [ ] Biome data access API integration
- [ ] Biome color mapping system

#### ❌ Critical Missing Systems

- [ ] Chunk access control system (**highest priority** - custom implementation with mixins)
- [ ] Movement prevention and interaction blocking
- [ ] Initial spawn chunk setup
- [ ] Player interaction UI
- [ ] Admin command system

#### ❌ Future Features

- [ ] Interactive chunk map (chunk selection interface)
- [ ] HUD display for credits
- [ ] Custom visual effects (gray haze shader, particles)
- [ ] Nether portal mechanics (auto-unlock if credits, wither damage if not)
- [ ] Dimensional exemptions (Nether/End free movement)
- [ ] Sound effects and screen effects for borders

---

### 📊 Completion Status

| Phase                          | System Status | User-Facing Status |
| ------------------------------ | ------------- | ------------------ |
| Phase 1: Core Systems          | 70% Complete  | 0% Usable          |
| Phase 1A: Game Modes           | 0% Complete   | 0% Usable          |
| Phase 1B: Biome Discovery      | 0% Complete   | 0% Usable          |
| Phase 2: Chunk Access Control  | 0% Complete   | 0% Usable          |
| Phase 3: UI & Visualization    | 0% Complete   | 0% Usable          |
| Phase 4: Dimensional Mechanics | 0% Complete   | 0% Usable          |
| Phase 5: Polish & Testing      | 60% Complete  | 0% Usable          |

**Overall Project**: ~35% code complete, **0% playable**

---

### 🎯 Roadmap to Alpha (Minimum Playable Version)

**Target**: Basic chunk progression gameplay loop

1. **Chunk Access Control System** (ETA: 4-6 days)

   - Implement ChunkAccessManager (per-player chunk tracking)
   - Movement prevention mixin (ServerPlayerEntity)
   - Interaction blocking mixins (block/entity)
   - Visual feedback (messages, particles)
   - Server-side validation

2. **Initial Spawn Setup** (ETA: 1 day)

   - Auto-unlock 2x2 chunk area at spawn
   - First-join detection and setup

3. **Admin Commands** (ETA: 1-2 days)

   - `/chunklocked credits <player> <amount>`
   - `/chunklocked unlock <x> <z>`
   - `/chunklocked list`

4. **Basic Chunk Selection** (ETA: 2-3 days)
   - Simple UI to list adjacent chunks
   - Click to unlock (spend credits)
   - Update world border on unlock

**Estimated Time to Alpha**: 7-11 days

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit pull requests or open issues for bugs and feature requests.

## Author

**JordanMartinWebDev**

---

_Built with ❤️ using Fabric_
