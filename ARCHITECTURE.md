# Architecture

This document is the navigation map for the codebase. Read it before broadly searching the repository.

## Package root

```text
com.willian.fieldengineering
```

## Entry points

### `FieldEngineering`
Main Fabric initializer. Registers items, blocks, block entities, entities, sounds, and item groups.

### `FieldEngineeringClient`
Client initializer. Registers client-only rendering such as the missile renderer.

## Main systems

### Radar
Primary files:

```text
block/RadarBlock.java
blockentity/RadarBlockEntity.java
target/TargetRules.java
```

Current responsibilities:

- scan for eligible living targets;
- range: 100 blocks;
- scan interval: 10 ticks;
- require open sky;
- ignore ordinary low-value mobs;
- expose the currently selected target to launchers;
- render debug electric-spark particles on a selected target.

Current target classes include Pillager, Vindicator, Evoker, Witch, Ravager, Wither, Ender Dragon, Warden, Elder Guardian, and Piglin Brute.

A target is considered airborne when it is at least two blocks above detected ground. This is intentionally based on spatial state rather than an AirRaid-specific API.

### Missile launcher
Primary files:

```text
block/AATurretBlock.java
block/LauncherPartBlock.java
blockentity/LauncherBlockEntity.java
blockentity/BlockEntitySearch.java
```

Physical structure:

- one master block;
- 3x3 footprint;
- two blocks high;
- 17 invisible auxiliary blocks reserve the remaining volume;
- breaking an auxiliary part destroys the master;
- the master removes the auxiliary parts when removed.

The master owns the block entity and launcher state.

#### Current launch sequence

```text
t = 0      static sound + preparation smoke
t = 80     launch burst + launch sound + ascending missile
t = 120    ascending visual missile is gone
t = 260    lock target / last known position
             ground target: wait 22 ticks
             air target: no additional wait
spawn descending missile + explosion sound
after 18 descending ticks: explosion at locked coordinate
```

The explosion damages entities and causes knockback but does not destroy terrain.

The launcher tracks the active target and continuously stores its last known coordinates before lock so a strike can complete after target loss/death.

### Missile entity
Primary file:

```text
entity/MissileEntity.java
```

Responsibilities:

- render as a projectile entity;
- ascending visual phase;
- descending visual phase;
- dense firework/smoke/flame trail;
- impact at the locked coordinate;
- vanilla explosion with terrain destruction disabled.

Current missile ownership is not attributed to the player. Player kill credit is a future requirement.

### Targeting rules
Primary file:

```text
target/TargetRules.java
```

Central place for target eligibility and air/ground classification. New targeting behavior should be concentrated here or in a dedicated targeting service rather than duplicated across radar and launcher code.

### Registries
Primary package:

```text
registry/
```

Contains block, block entity, entity, item, item group, and sound registration.

### Resources
Primary root:

```text
src/main/resources/assets/field-engineering/
```

Contains blockstates, models, language entries, sounds, textures, and `sounds.json`.

## Critical performance issue

`BlockEntitySearch`/launcher radar discovery currently performs a brute-force spatial scan over block positions in a large radius. At radius 128, a cube can contain roughly 17 million block positions before considering multiple launchers or repeated ticks.

This implementation is not suitable for production-scale use and is the current highest-priority technical problem.

Planned direction:

```text
radar placed/loaded
    -> register position

radar removed/unloaded
    -> unregister position

launcher needs radar
    -> query registered radar positions only
    -> filter by dimension/range/state
    -> cache selected radar

cached radar invalid
    -> resolve another registered radar
```

Do not replace this with another world-wide or chunk-wide repeated scan.

## Planned systems

These are design directions, not necessarily implemented:

- air targets prioritized over ground targets;
- target reservation so multiple launchers do not waste missiles on the same target;
- staggered launches;
- ammunition consumption;
- launcher owner UUID and player kill attribution;
- anti-missile interceptors;
- hostile Illager radar/missile batteries;
- retrofit of hostile installations into existing generated worlds;
- artillery/saturation batteries with a player designator;
- tactical Illager AI including preferred range, retreat, and cover;
- richer spatial/mechanical audio including distance and Doppler behavior.

## Architectural rule

Game rules should be explicit and centralized. The user defines how systems should behave; implementation should preserve those rules without inventing hidden behavior. When a rule becomes shared by multiple systems, promote it into a dedicated service/rules class rather than copying conditionals across block entities.
