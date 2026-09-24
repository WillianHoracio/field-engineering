# Project State

Update this file whenever a meaningful feature is completed, removed, or changes direction.

## Working baseline

- Fabric 1.21.1 development environment works.
- Java 21 works.
- Gradle wrapper is pinned to 8.10.2.
- Loom 1.8.13 builds a valid production JAR.
- Production artifact: `build/libs/field-engineering-1.0.0.jar`.
- The mod loads in the real launcher/modpack.
- The performance foundation refactor has been merged to `main`.

## Current branch under test

```text
fix/issue-2-missile-long-range-visuals
```

The branch was reset to `main` after the previous tracking/remote-visual experiments did not improve the real result. The current approach keeps the original simulation model but extends the visible flight envelope and uses a dedicated renderer for long-range readability.

## Implemented

### Radar

- 100-block target range.
- Open-sky requirement.
- High-value hostile target allow-list.
- Air/ground classification.
- Radar coordinates registered per dimension/server level.
- On-demand entity scanning instead of continuous autonomous scanning.
- Scan results cached for 20 ticks so multiple launchers can reuse one scan.
- Debug particles remain limited to one detected target per scan.

### Launcher

- 3x3x2 multiblock footprint.
- Central master block with invisible structural parts.
- Radar discovery through `RadarRegistry`; no block-volume brute-force search.
- Radar range check is horizontal (X/Z) and uses squared distance.
- Idle target acquisition occurs approximately every 5 seconds.
- Each launcher has its own deterministic timing offset to avoid synchronized fire cycles.
- Targets from all in-range radars are merged and deduplicated by UUID.
- Air targets are preferred when available.
- Within a priority class, the closest available target is selected.
- Target reservation prevents multiple launchers from wasting missiles on the same entity.
- Preparation smoke and launch effects.
- Ascending missile persists until the descending phase actually begins.
- Launcher stores the ascending missile UUID and discards it on descent/cancel/removal.
- Delayed strike phase.
- Last-known target coordinates.
- Last-known air/ground state survives target death before lock.
- Different lock-to-descent delay for air and ground targets.
- Cooldown.

### Missile

- Ascending missile no longer despawns after 40 ticks.
- It continues upward at 1.8 blocks/tick until `maxBuildHeight - 2` and then waits there until descent begins.
- Descending missile now spawns at `maxBuildHeight - 2` instead of 90 blocks above impact.
- Descent still lasts exactly 18 ticks; velocity increases automatically to cover the larger distance.
- Final impact is still snapped to the locked coordinate before explosion, preserving precision.
- Original close-range firework/smoke/flame particles preserved.
- Custom client renderer replaces the plain `ThrownItemRenderer`.
- Vanilla firework body is rendered at 2x visual scale for readability.
- Renderer adds a layered 8-block velocity-aligned trail directly in the render pipeline.
- The renderer preserves a vertical trail when the ascending missile is stationary at the world ceiling.
- The direct trail does not use the vanilla particle system and therefore is not subject to particle render-distance limits.
- Explosion damages entities without terrain destruction.

### Timing

The strike timing itself is unchanged from the validated baseline:

```text
launch -> 180 ticks (9.0 s) -> target lock / air descent
                         +22 ticks (1.1 s) -> ground descent
                         +18 ticks (0.9 s) -> impact after descent begins
```

The 180 ticks are the previous 40-tick ascent phase plus the 140-tick (7 second) post-ascent wait. The visual ascent now fills that previously empty interval instead of disappearing after 2 seconds.

### Audio

Current sound assets include:

```text
static.ogg
launch.ogg
explosion.ogg
```

Current audio works, but future design may add entity-attached loops, distance layers, directional behavior, and Doppler-like pitch changes.

## Previous issue #2 experiments

Two approaches were tested and rejected before the current renderer-based implementation:

1. increasing missile tracking/render range and moving the particle trail client-side;
2. a remote deterministic missile-visual manager independent of physical entity tracking.

Real-world testing showed no useful improvement, and the second approach made the visual result worse. The clean launcher test then showed that the vanilla rocket entity itself remains visible at useful distance while the particle trail disappears much earlier. This redirected the fix toward direct rendering rather than networking/tracking changes.

## Real-world test to perform

1. confirm the close-range launch still looks and behaves like `main`;
2. verify the vanilla rocket body is visibly larger but still recognizable;
3. confirm the ascending missile stays visible beyond the old 40-tick despawn point;
4. confirm it reaches the world ceiling and remains there until descent begins;
5. observe the missile at increasing distance and verify the direct trail remains visible after vanilla particles disappear;
6. test both air-target and ground-target timing;
7. verify the descending missile starts at world ceiling and still impacts exactly after 18 ticks;
8. verify impact point, damage, audio, and target reservation behavior are unchanged;
9. fire several missiles and check client FPS/server responsiveness.

Watch specifically for:

- ascending missile disappearing before descent begins;
- unexpected overlap between ascending and descending missiles;
- descending missile missing its locked impact point;
- trail disappearing at the same distance as vanilla particles;
- trail not aligning with missile movement;
- line width behaving differently with Sodium/Iris/shaders;
- excessive visual thickness close to the camera;
- altered missile behavior unrelated to rendering.

## Known/intentional constraints

- Radar scans still use Minecraft entity AABB queries; this is expected to be much cheaper than block-volume scanning and is currently cached/shared.
- Existing vanilla particles are intentionally retained as close-range detail for now.
- The new direct trail still depends on the missile entity itself being rendered; current testing indicates the entity remains visible farther than the particles.
- Missile model replacement/refinement is deferred to a separate future PR.
- PvE is the priority; multiplayer is not currently a design target.
- Existing lock/descent timing is intentionally preserved even though the visible ascent now lasts longer.
- World-generation changes should avoid visible seams in already-generated large worlds.
- Integrations should avoid hard dependencies on another mod's internals when a robust generic rule is sufficient.
- The user owns gameplay/system design decisions. Code should implement those decisions rather than silently redefining them.

## Next gameplay work after approval

Potential sequence:

1. ammunition and ownership/player kill attribution;
2. interceptors;
3. hostile Illager batteries;
4. tactical Illager AI;
5. siege/game-mode systems and military points of interest.

This list can change according to user design decisions.
