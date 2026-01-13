# Copilot Instructions - Chunk Locked

## Project Overview

**Chunk Locked** is a Minecraft Fabric mod that implements chunk-based progression. Players start with a 2x2 chunk area and unlock additional chunks by completing advancements.

### Technical Stack

- **Minecraft Version**: 1.21.11
- **Mod Loader**: Fabric 0.18.4
- **Java Version**: 21
- **Fabric API**: 0.141.1+1.21.11
- **Build Tool**: Gradle
- **Mappings**: **Mojang Official Mappings** (NOT Yarn - see warning below)

---

## 🚨 CRITICAL: Mojang Mappings ONLY - NO YARN

**This project uses Mojang official mappings exclusively. NEVER use Yarn mappings.**

### Why This Matters

We migrated from Yarn to Mojang mappings and it was extremely painful. Method names, field names, and class names are DIFFERENT between Yarn and Mojang. Mixing them causes compilation failures and runtime crashes.

### build.gradle Configuration

```gradle
loom {
    officialMojangMappings()  // ← MUST use this, NEVER use yarn mappings
}
```

### Common Yarn → Mojang Differences

| Yarn Name | Mojang Name |
|-----------|-------------|
| `getYaw()` | `getYRot()` |
| `getPitch()` | `getXRot()` |
| `player.age` | `player.tickCount` |
| `getEntityWorld()` | `level()` |
| `getBlockPos()` | `blockPosition()` |
| `addStatusEffect()` | `addEffect()` |
| `hasStatusEffect()` | `hasEffect()` |
| `removeStatusEffect()` | `removeEffect()` |
| `sendMessage(text, false)` | `sendSystemMessage(text)` |
| `ResourceKey.location()` | `ResourceKey.identifier()` |
| `grantCriterion` | `award` |
| `getProgress` | `getOrStartProgress` |
| `owner` (PlayerAdvancements) | `player` |
| `teleportTo(TeleportTransition)` | `teleport(TeleportTransition)` |
| `getId()` (packets) | `type()` |
| `ID` (packet constant) | `TYPE` |

### Block Registration in MC 1.21+

In Minecraft 1.21+, you MUST call `setId()` on `BlockBehaviour.Properties` before constructing a Block:

```java
Identifier blockId = Identifier.fromNamespaceAndPath(MOD_ID, "my_block");
ResourceKey<Block> blockKey = ResourceKey.create(BuiltInRegistries.BLOCK.key(), blockId);

Block MY_BLOCK = Registry.register(
    BuiltInRegistries.BLOCK,
    blockKey,
    new MyBlock(BlockBehaviour.Properties.of()
        .setId(blockKey)  // ← REQUIRED in 1.21+
        .strength(1.0f)
        .noOcclusion()));
```

### If You're Unsure About a Method Name

Use `javap` to inspect the actual Minecraft JAR:

```bash
cd ~/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-common/1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2
javap -classpath minecraft-common-*.jar net.minecraft.server.level.ServerPlayer | grep methodName
```

### NEVER Do These Things

- ❌ Never add `yarn_mappings` to build.gradle
- ❌ Never use Fabric's `migrateMappings` to convert TO Yarn
- ❌ Never copy code from Yarn-based mods without converting method names
- ❌ Never assume Yarn documentation applies to this project

---

## 📋 Progress Tracking Workflow

### Three-Tier Documentation System

We maintain a **bottom-to-top** tracking approach with three levels of documentation:

#### 1. **Implementation Plans** (Detailed Task Level)

- **Location**: `docs/[phase]/[system]/implementation-plan.md`
- **Purpose**: Track individual tasks, code files, and technical details
- **When to Update**: Mark tasks as complete (`[x]`) as you implement each piece of code
- **Scope**: Granular - individual methods, test files, configuration steps

**Example**: `docs/core-systems/advancement-integration/implementation-plan.md`

#### 2. **Overview Documentation** (System Level)

- **Location**: `docs/00-overview.md`
- **Purpose**: Track completion of major systems and subsystems
- **When to Update**: Mark systems complete when ALL implementation plan tasks are done
- **Scope**: High-level - entire packages, major features, system integration

**Format**:

```markdown
- ✅ Advancement integration (complete)
- ⚠️ World border management (75% complete)
- ❌ Portal mechanics (not started)
```

#### 3. **README.md** (User-Facing Features)

- **Location**: `README.md`
- **Purpose**: Track what players can actually USE in-game
- **When to Update**: Mark features complete ONLY when playable/usable by end users
- **Scope**: User features - things players interact with, see, or experience

**Format**: Separate "User-Facing Features" (playable) from "Backend Systems" (implemented but not exposed)

### Update Workflow

When implementing a feature, follow this sequence:

1. **Start**: Check implementation plan for task list
2. **During Development**: Mark individual tasks `[x]` in implementation plan as completed
3. **System Complete**: When all tasks done, update `docs/00-overview.md` with `✅`
4. **User-Ready**: When system has UI/commands/gameplay, update `README.md`

### Example Workflow

```
1. Task: Implement AdvancementCreditManager.onAdvancementCompleted()
   ↓
   Mark in advancement-integration/implementation-plan.md:
   - [x] Implement onAdvancementCompleted() method

2. All Phase 2 tasks complete
   ↓
   Update docs/00-overview.md:
   - ✅ Advancement integration (credit system working)

3. Add `/chunklocked credits` command so players can see credits
   ↓
   Update README.md:
   #### ✅ Credit Display (Usable)
   - [x] Players can check their credits with /chunklocked credits
```

### Status Markers

Use consistent markers across all documents:

- ✅ **Complete** - Fully implemented and tested
- ⚠️ **Partial** - Some parts done, some missing (show percentage)
- 🚧 **In Progress** - Currently being worked on
- ❌ **Not Started** - Planned but not begun
- 🔴 **Blocking** - Critical issue preventing progress
- ⏳ **Deferred** - Intentionally postponed

### Documentation Update Checklist

Before considering ANY feature "done":

- [ ] All tasks marked `[x]` in implementation plan
- [ ] Implementation plan shows phase as "COMPLETE"
- [ ] `docs/00-overview.md` updated with completion status
- [ ] `README.md` updated IF user-facing functionality exists
- [ ] Manual tests documented in `manual-tests/`
- [ ] API documentation created in `Documentation/` (if public API)

### Documentation During Implementation

**CRITICAL**: Update documentation AS YOU CODE, not after. This prevents documentation drift and ensures accuracy.

#### When Implementing a New Feature:

1. **Before Writing Code**:

   - Check if implementation plan exists in `docs/[phase]/[system]/implementation-plan.md`
   - If not, create one with task breakdown
   - Review planned architecture and approach

2. **While Writing Code**:

   - Add Javadoc comments to ALL public methods and classes
   - Document complex algorithms with inline comments
   - Mark tasks as complete `[x]` in implementation plan as you finish them
   - Update Architecture.md if design changes from original plan

3. **After Each Phase/System Complete**:
   - Create API documentation in `Documentation/[System]Name.md`
   - Update `docs/00-overview.md` to reflect system completion
   - Create manual test cases in `manual-tests/[FeatureName].md`
   - Write unit tests (if testable without Minecraft bootstrap)
   - Update README.md if the feature is user-facing

#### Documentation Requirements by Code Type:

**Public APIs** (must document):

- Purpose and responsibilities
- Method parameters with `@param`
- Return values with `@return`
- Exceptions with `@throws`
- Usage examples
- Thread safety notes

**Internal Classes** (document if complex):

- Class-level Javadoc with purpose
- Complex algorithm explanations
- Performance considerations
- Any non-obvious behavior

**Configuration** (always document):

- Default values
- Valid ranges
- Impact on gameplay
- Example configurations

#### Documentation Structure for New Systems:

```markdown
# [System Name]

## Overview

Brief description of what this system does.

## Architecture

- Key classes and responsibilities
- Component interactions
- Data flow

## API Reference

Public methods with parameters, returns, examples

## Configuration

Configurable options and defaults

## Testing

Test coverage and manual test instructions

## Known Limitations

Current constraints or future improvements
```

#### Quick Documentation Checklist:

- [ ] Javadoc on ALL public classes/methods
- [ ] Complex logic has inline comments
- [ ] Configuration options documented
- [ ] Manual test cases written
- [ ] API docs created (if public API)
- [ ] Implementation plan tasks marked complete
- [ ] 00-overview.md updated (if system complete)
- [ ] README.md updated (if user-facing)

**Remember**: Good documentation is written DURING development, not AFTER. If you're writing code without updating docs, you're doing it wrong.

---

## Code Organization

```
src/main/java/chunkloaded/
├── core/           # Data structures, tracking, persistence
├── border/         # World border management
├── advancement/    # Advancement listening and credit logic
├── portal/         # Nether portal mechanics
├── command/        # Admin commands
├── config/         # Configuration handling
└── mixin/          # Mixins for Minecraft modification

src/client/java/chunkloaded/
├── ui/            # Map interface, HUD elements
├── render/        # Visual effects, shaders
└── mixin/client/  # Client-side mixins

src/test/java/chunkloaded/
└── [mirrors main structure for tests]
```

---

## Testing Requirements

### Mandatory Unit Testing

**Every function must have unit tests.** This is non-negotiable.

- **Target Coverage**: 80% code coverage minimum for testable code
- **Test Location**: Mirror main source structure in `src/test/java/chunkloaded/`
- **Test Framework**: JUnit 5
- **Naming Convention**: `ClassNameTest.java` for testing `ClassName.java`
- **Method Naming**: Use descriptive test names: `testMethodName_Scenario_ExpectedResult()`

### Unit Testing Limitations

**Important**: Standard unit tests cannot use Minecraft classes that require full game initialization (like `ChunkPos`, `ServerPlayerEntity`, etc.) due to complex bootstrap requirements.

**What CAN be tested with unit tests:**

- Business logic classes (credit calculation, data structures)
- Configuration handling
- NBT serialization/deserialization logic (without ChunkPos)
- Utility methods and algorithms
- Data validation and error handling

**What REQUIRES integration/manual testing:**

- Any code using `ChunkPos` or Minecraft world objects
- Event handlers and mixins
- UI components
- Network synchronization
- World border manipulation

For features requiring Minecraft classes, rely on:

1. **Manual testing** - documented test cases in `manual-tests/`
2. **Integration tests** - using Fabric's game test framework (future work)
3. **In-game verification** - testing in actual Minecraft environment

### Unit Test Examples

```java
// For: ChunkManager.unlockChunk(ChunkPos pos)
@Test
void unlockChunk_ValidPosition_ReturnsTrue() { }

@Test
void unlockChunk_AlreadyUnlocked_ReturnsFalse() { }

@Test
void unlockChunk_NullPosition_ThrowsException() { }
```

### Implementation Tests

When implementing features, include **integration/implementation tests** that verify the feature works end-to-end:

- Test actual Minecraft environment interactions
- Verify data persistence across save/load
- Test multiplayer synchronization
- Validate UI interactions
- Test cross-dimension behavior

### Test Coverage Guidelines

**Adjusted for Unit Test Constraints:**

- **Pure Business Logic**: 85%+ coverage (config, credit management, data structures)
- **Serialization Logic**: 80%+ coverage (NBT handling without Minecraft types)
- **Utility Classes**: 90%+ coverage (algorithms, validators, helpers)
- **Minecraft-Integrated Code**: Manual testing only (chunk tracking, area detection, managers)

**Overall realistic target: 80% for testable code** (excluding Minecraft-dependent classes)

### What to Mock

- Minecraft server/client instances (when testing in isolation)
- File I/O for persistence tests
- Network packets for multiplayer tests
- Advancement system for credit tests

### What NOT to Mock

- Core business logic (test the real implementations)
- Simple utility methods

### What CANNOT be Unit Tested

Due to Minecraft bootstrap complexity, the following require manual/integration testing:

- Classes using `ChunkPos`, `ServerPlayerEntity`, or other Minecraft world objects
- World border management
- Area detection algorithms (BFS on chunk coordinates)
- Chunk unlock/lock operations
- Manager facades that orchestrate Minecraft objects

These should have:

- **Manual test cases** documented in `manual-tests/`
- **Integration tests** using Fabric's game test framework (when implemented)
- **In-game verification** during development

### Manual Testing

**Every implementation must include manual test cases for user verification.**

- **Test Location**: Create test documentation in `manual-tests/` folder
- **Format**: Markdown files with step-by-step instructions
- **Naming Convention**: `FeatureName.md` (e.g., `ChunkUnlocking.md`, `PortalMechanics.md`)
- **Content**: Clear steps, expected results, edge cases to verify
- **Test Runner**: Add tests to `manual-tests/test-runner.html` for interactive tracking

#### Manual Test Workflow

1. **Create Markdown Documentation**: Write detailed test cases in a `.md` file
2. **Add to Test Runner**: Update `test-runner.html` JavaScript array with test data
3. **Execute Tests**: Open `test-runner.html` in browser and check off completed tests
4. **Track Progress**: Test runner saves progress in browser localStorage

#### Adding Tests to Test Runner

When creating new manual tests in markdown files, regenerate the HTML test runner:

```bash
cd manual-tests
node generate-test-runner.js
```

The script automatically parses all `.md` files in `manual-tests/` and generates `test-runner.html` with all test cases. The markdown parser expects this structure:

```markdown
# Feature Name - Manual Test Cases

## Test Case 1: Title Here

**Prerequisites**: Setup needed
**Steps**:

1. Step one
2. Step two
   **Expected Result**: What should happen
   **Edge Cases**: Variations to test
```

#### Manual Test Template

```markdown
# Feature Name - Manual Test Cases

## Test Case 1: [Description]

**Prerequisites**: [What needs to be set up]
**Steps**:

1. Step one
2. Step two
3. Step three

**Expected Result**: [What should happen]
**Edge Cases**: [Variations to test]

## Test Case 2: [Description]

...
```

---

## Documentation Requirements

### Full Implementation Documentation

**Every complete feature implementation must include comprehensive documentation.**

#### Documentation Structure

Create documentation in a new folder under `Documentation/`:

```
Documentation/
├── CoreSystems/
│   ├── ChunkTracking.md
│   ├── AdvancementIntegration.md
│   └── Persistence.md
├── WorldBorder/
│   ├── BorderManagement.md
│   └── MultiAreaSupport.md
├── UI/
│   ├── ChunkMap.md
│   └── HUDElements.md
└── DimensionalMechanics/
    ├── PortalMechanics.md
    └── AutoUnlock.md

manual-tests/
├── ChunkUnlocking.md
├── PortalMechanics.md
├── WorldBorder.md
└── UI_Testing.md
```

#### Documentation Template

Each documentation file should include:

```markdown
# Feature Name

## Overview

Brief description of what this feature does.

## Architecture

- Key classes and their responsibilities
- Data flow diagrams (if complex)
- Dependencies on other systems

## Implementation Details

- How it works internally
- Important algorithms or logic
- Performance considerations

## API Reference

Public methods/classes with parameters and return types.

## Configuration

Any configurable options and their defaults.

## Testing

- Test coverage percentage
- Key test scenarios covered
- Integration test locations

## Known Limitations

Current constraints or planned improvements.

## Examples

Code examples showing typical usage.
```

### Code Documentation

- **All public methods**: Include Javadoc with `@param`, `@return`, `@throws`
- **Complex algorithms**: Inline comments explaining logic
- **Mixins**: Document why the mixin is needed and what it modifies
- **Magic numbers**: Extract to named constants with comments

---

## Coding Standards

### Naming Conventions

- **Classes**: PascalCase (`ChunkManager`, `AdvancementListener`)
- **Methods**: camelCase (`unlockChunk`, `calculateBorder`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_AREAS`, `DEFAULT_CREDITS`)
- **Packages**: lowercase (`core`, `border`, `advancement`)
- **Test Classes**: `ClassNameTest`
- **Test Methods**: `testMethod_Scenario_Expected` or descriptive names

### Java Best Practices

- **Null Safety**: Use `@Nullable` and `@NotNull` annotations
- **Immutability**: Prefer immutable data structures where possible
- **Error Handling**: Fail gracefully with clear error messages
- **Logging**: Use SLF4J for logging important events
- **Resource Management**: Use try-with-resources for I/O operations
- **Thread Safety**: Document thread safety assumptions

### Fabric-Specific Practices

- **Event Listeners**: Use Fabric API events, not Forge-style annotations
- **Mixins**: Use sparingly; prefer Fabric API when possible
- **Client/Server Split**: Always consider sided execution
- **Networking**: Use `ServerPlayNetworking` and `ClientPlayNetworking`
- **Data Persistence**: Use `PersistentState` for server-side data

---

## What We Are Building

### Core Systems (Phase 1)

- **Chunk Tracking**: Data structure for locked/unlocked chunks per player
- **Advancement Integration**: Listen to advancement completions, grant credits
- **World Border Management**: Dynamic world borders based on unlocked chunks
- **Multi-Area Support**: Handle disconnected playable regions
- **Persistence**: Save/load chunk states with world data

### UI & Visualization (Phase 2)

- **Interactive Chunk Map**: UI for viewing and selecting chunks
- **Visual Indicators**: In-world rendering of borders
- **Credit Display**: HUD showing available unlock credits
- **Gray Haze Shader**: Visual effect for locked areas

### Dimensional Mechanics (Phase 3)

- **Overworld Enforcement**: World border restrictions in Overworld only
- **Nether/End Exemption**: Free exploration in other dimensions
- **Portal Detection**: Track when players exit Nether portals
- **Auto-Unlock System**: Spend credits when exiting to locked chunks
- **Poison Damage**: Penalty for invalid portal exits

### Gameplay Features (Phase 4)

- **Initial Spawn Setup**: Configure 2x2 starting area
- **Adjacency Validation**: Ensure unlocked chunks are connected
- **Chunk Selection UI**: Interface for spending unlock credits
- **Player Feedback**: Messages, sounds, visual effects

---

## What We Are NOT Building

### Explicitly Out of Scope

- ❌ Economy/currency systems (no emerald costs)
- ❌ Land claiming or grief protection
- ❌ Time-gated progression or cooldowns
- ❌ Chunk maintenance costs or decay
- ❌ World generation modification
- ❌ Custom crafting or mob spawning changes
- ❌ New items/blocks (except maybe teleport item later)
- ❌ Custom dimensions or pocket dimensions
- ❌ Full minimap functionality (just unlock status)
- ❌ Bedrock Edition or backporting to older versions

---

## Implementation Workflow

### For Every New Feature

1. **Design**: Review relevant documentation in `docs/`
2. **Implement**: Write the feature code
3. **Test**: Write unit tests achieving 90%+ coverage
4. **Integration Test**: Write implementation tests
5. **Manual Tests**: Create manual test cases in `manual-tests/` folder
6. **Document**: Create documentation in `Documentation/` folder
7. **Verify**: Run full test suite, check coverage
8. **Log**: Add appropriate logging statements
9. **Review**: Ensure code follows standards

### For Every Method

1. Write the method signature and Javadoc
2. Implement the method logic
3. Write at least 3 unit tests (happy path, edge cases, error cases) **IF the method doesn't require Minecraft bootstrap**
4. For Minecraft-dependent methods, write manual test cases instead
5. Verify test coverage meets target threshold

### For Every Bug Fix

1. Write a failing test that reproduces the bug
2. Fix the bug
3. Verify the test passes
4. Add additional tests for related edge cases

---

## Performance Considerations

- **Chunk Operations**: O(1) lookup time for unlock status
- **World Border Updates**: Batch updates, avoid per-tick recalculation
- **UI Rendering**: Optimize map rendering, cache chunk textures
- **Data Persistence**: Async saves, don't block main thread
- **Network Sync**: Only sync changes, not full state every tick

---

## Multiplayer Considerations

- **Credit System**: Decide if credits are per-player or shared
- **Chunk Unlocks**: Ensure atomic operations, prevent race conditions
- **Network Sync**: Keep clients informed of unlock changes
- **Server Authority**: Server validates all unlock requests
- **Player Joins**: Sync current unlock state to joining players

---

## Key Technical Decisions Needed

Document decisions in implementation documentation:

- Credit System: Per-player or shared in multiplayer?
- Border Implementation: Native WorldBorder or custom system?
- Map Rendering: 2D overlay or 3D in-world?
- Data Storage: NBT or JSON format?
- Shader Approach: Post-processing or fragment shader?

---

## Useful Resources

- [Fabric Wiki](https://fabricmc.net/wiki/)
- [Fabric API Javadoc](https://maven.fabricmc.net/docs/fabric-api-latest/)
- [Mixin Documentation](https://github.com/SpongePowered/Mixin/wiki)
- [Fabric Discord](https://discord.gg/v6v4pMv)

---

## Version Control

- **Branch Naming**: `feature/feature-name`, `bugfix/issue-description`
- **Commit Messages**: Follow conventional commits format
- **Pull Requests**: Include test coverage report
- **Code Review**: Required before merge to main

---

## Common Patterns

### Data Persistence

```java
public class ChunkUnlockData extends PersistentState {
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        // Serialize data
    }

    public static ChunkUnlockData fromNbt(NbtCompound nbt) {
        // Deserialize data
    }
}
```

### Event Registration

```java
public void onInitialize() {
    AdvancementGrantCallback.register((player, advancement, criterionName) -> {
        // Handle advancement
    });
}
```

### Client-Server Communication

```java
// Server -> Client
ServerPlayNetworking.send(player, UNLOCK_PACKET, buf -> {
    buf.writeChunkPos(pos);
});

// Client receives
ClientPlayNetworking.registerGlobalReceiver(UNLOCK_PACKET, (client, handler, buf, responseSender) -> {
    ChunkPos pos = buf.readChunkPos();
    // Handle on client
});
```

---

## Testing Dependencies

Add to `build.gradle` as needed:

```gradle
dependencies {
    testImplementation 'org.junit.jupiter:junit-jupiter:5.9.0'
    testImplementation 'org.mockito:mockito-core:5.0.0'
    testImplementation 'org.mockito:mockito-junit-jupiter:5.0.0'
}
```

---

## Quality Gates

Before considering any feature complete:

- ✅ All unit tests pass
- ✅ 80%+ code coverage achieved for testable code
- ✅ Integration tests written and passing (if applicable)
- ✅ Manual test cases documented in `manual-tests/` folder
- ✅ Manual testing completed and verified
- ✅ Documentation created in `Documentation/` folder
- ✅ Javadoc complete for public APIs
- ✅ No compiler warnings
- ✅ Multiplayer tested (if applicable)
- ✅ Performance profiled (if relevant)

---

## Remember

- **Test first, then implement** (or at least test immediately after)
- **Document as you build**, not after
- **Coverage is not just a number** - test meaningful scenarios
- **Fail fast** with clear error messages
- **Log important events** for debugging
- **Think multiplayer** from the start
- **Consider performance** in hot paths
- **Keep it focused** - refer to "What We Are NOT Building"
