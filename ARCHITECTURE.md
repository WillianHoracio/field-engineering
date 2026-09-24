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
tracking/RadarRegistry.java
target/TargetRules.java
```

Responsibilities:

- register loaded radar coordinates by server level/dimension;
- scan for eligible living targets only when a launcher requests data;
- cache scan results for 20 ticks so nearby launchers can reuse one scan;
- range: 100 blocks;
- require open sky;
- ignore ordinary low-value mobs;
- return all currently valid detected targets to launchers;
- keep debug electric-spark feedback limited to the nearest detected target.

The registry stores coordinates rather than permanent `BlockEntity` references. Launchers first perform cheap X/Z distance checks against registered coordinates, then resolve only in-range radar block entities.

Current target classes include Pillager, Vindicator, Evoker, Witch, Ravager, Wither, Ender Dragon, Warden, Elder Guardian, and Piglin Brute.

A target is considered airborne when it is at least two blocks above the motion-blocking surface. Because valid targets must be under open sky, heightmap lookup replaces the old downward block-by-block scan.

### Missile launcher
Primary files:

```text
block/AATurretBlock.java
block/LauncherPartBlock.java
blockentity/LauncherBlockEntity.java
tracking/RadarRegistry.java
tracking/TargetReservationRegistry.java
```

Physical structure:

- one master block;
- 3x3 footprint;
- two blocks high;
- 17 invisible auxiliary blocks reserve the remaining volume;
- breaking an auxiliary part destroys the master;
- the master removes the auxiliary parts when removed.

The master owns launcher state.

#### Target acquisition

Idle launchers do not search every tick.

```text
launcher idle
    -> wait for its own acquisition timer (~5 s cycle)
    -> query registered radars within 128 blocks horizontally
    -> request cached/on-demand detections from those radars
    -> merge targets and remove duplicate UUIDs
    -> ignore targets reserved by another launcher
    -> prefer an airborne target if available
    -> otherwise choose the nearest available ground target
    -> reserve target
    -> begin preparation
```

Each launcher starts with a deterministic timing offset derived from its block position, so multiple launchers do not naturally enter synchronized acquisition/fire cycles.

#### Target reservation

`TargetReservationRegistry` is scoped per `ServerLevel`.

```text
target UUID -> launcher block position + timeout
```

A target can be reserved by only one launcher at a time. Other launchers move to the next available target instead of wasting missiles on the same entity. Reservations are released when engagement finishes/cancels or when the launcher is removed. A timeout protects against stale entries.

#### Launch sequence

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

The launcher tracks the active target and continuously stores its last known coordinates before lock so a strike can complete after target loss/death. Air/ground state is also stored with the last-known target state, so a target that dies before lock keeps the correct descent timing.

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

### Runtime tracking
Primary package:

```text
tracking/
```

Contains lightweight runtime indexes and coordination state. These systems must stay dimension-scoped and must clean up stale/unloaded state.

### Resources
Primary root:

```text
src/main/resources/assets/field-engineering/
```

Contains blockstates, models, language entries, sounds, textures, and `sounds.json`.

## Performance architecture

The removed `BlockEntitySearch` implementation scanned every block position in a large 3D volume. At radius 128 this could approach 17 million block positions per launcher search, which scaled catastrophically with multiple launchers.

Current rule:

```text
never rediscover sparse known systems by scanning world volume
```

Prefer:

```text
lifecycle/event -> registry/index -> cheap mathematical filter -> resolve only relevant objects
```

For periodic systems, spread work across time and cache shared results rather than making every instance independently repeat the same query.

## Planned systems

These are design directions, not necessarily implemented:

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
