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

Physical structure on the model-pass branch:

- one master block at the center of the lower layer;
- 3x3 footprint;
- three blocks high;
- 26 auxiliary structural blocks reserve the remaining volume;
- every auxiliary part stores `offset_x`, `offset_y`, and `offset_z` relative to the master;
- auxiliary parts render local model slices so the full launcher stays within valid per-block model bounds;
- breaking an auxiliary part destroys the master;
- the master removes the auxiliary parts when removed.

The master owns launcher state. Structural parts contain only placement/visual identity and master-relative offsets; they do not own launcher simulation state.

#### Multiblock visual model

The static SAM launcher model is distributed across the 3x3x3 structure:

```text
lower layer: heavy fixed platform/base + master machinery
middle layer corners: lower halves of four grouped vertical canisters
middle layer edges/center: compact support and electronics pieces
upper layer corners: upper halves/caps of the four canisters
upper layer edges/center: visually empty reserved structure
```

The four canisters are intentionally pulled toward the middle of the installation so the silhouette reads as a fixed SAM battery rather than four unrelated corner towers.

Each block model remains inside normal local model bounds. This avoids Blockbench/Minecraft `model too large` problems while preserving the intended full installation scale.

Current launcher geometry uses vanilla textures as placeholders during shape validation. Dedicated textures should be added only after the geometry is approved ingame.

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
           ascending missile keeps climbing toward world ceiling
           if it reaches the ceiling first, it remains there visibly
t = 260    lock target / last known position
             air target: replace ascent with descending missile immediately
             ground target: wait 22 additional ticks, then replace it
spawn descending missile at world ceiling + explosion sound
after 18 descending ticks: explosion at locked coordinate
```

The historical strike timing is preserved: the old 40-tick ascent phase plus 140-tick wait still defines the 180-tick launch-to-lock interval. The difference is visual continuity: the ascending missile is no longer discarded after 40 ticks. The launcher owns its UUID and removes it exactly when the descending phase begins or when the strike is cancelled.

The descending missile starts at `maxBuildHeight - 2`. Its velocity is calculated from the full ceiling-to-impact distance so it still reaches the locked coordinate in 18 ticks. The final position is snapped to the authoritative impact coordinate before explosion, preserving precision.

The explosion damages entities and causes knockback but does not destroy terrain.

The launcher tracks the active target and continuously stores its last known coordinates before lock so a strike can complete after target loss/death. Air/ground state is also stored with the last-known target state, so a target that dies before lock keeps the correct descent timing.

### Missile entity and renderer
Primary files:

```text
entity/MissileEntity.java
client/render/MissileRenderer.java
models/item/aa_missile.json
```

Responsibilities:

- preserve the server-authoritative ascending and descending missile entity;
- allow ascent to continue to the world ceiling instead of self-despawning after two seconds;
- keep the approved firework/smoke/flame particles for close-range detail;
- render the mod's dedicated `aa_missile` 3D item model instead of a vanilla firework rocket;
- use a compact interceptor-style body, nose and tail fins at a scale consistent with the current TNT-like damage;
- render a direct client-side layered trail from missile velocity, independent of the vanilla particle-distance limit;
- preserve a vertical trail while a missile is stationary at the world ceiling without relying on unsynchronized server-only state;
- explicitly allow missile rendering up to 1024 blocks through `shouldRenderAtSqrDistance`;
- descend from the world ceiling to the exact locked coordinate in the existing 18-tick descent window;
- vanilla explosion with terrain destruction disabled.

The direct trail is part of the entity renderer rather than a particle stream. This means the long-range readability layer follows the missile whenever the entity itself is rendered, while the existing particles remain a local-detail effect.

Diagnostic testing showed that increasing entity tracking range did not change the disappearance point, while forcing `shouldRenderAtSqrDistance` to accept the entity kept the full trajectory visible. The final implementation therefore restores the normal 10-chunk tracking range and uses a bounded 1024-block render-distance override instead of extending tracking globally.

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

For missile visuals, prefer a bounded render-time effect derived from already-synchronized entity velocity over increasing continuous server particle traffic or extending entity tracking beyond what gameplay requires.

For multiblock visuals, prefer distributing normal local models across structural parts over rendering one giant model far outside a single block's normal bounds.

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
