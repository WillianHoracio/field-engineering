# Project State

Update this file whenever a meaningful feature is completed, removed, or changes direction.

## Working baseline

- Fabric 1.21.1 development environment works.
- Java 21 works.
- Gradle wrapper is pinned to 8.10.2.
- Loom 1.8.13 builds a valid production JAR.
- Production artifact: `build/libs/field-engineering-1.0.0.jar`.
- The mod loads in the real launcher/modpack.

## Current branch under test

```text
refactor/performance-foundation
```

This branch is intentionally focused on performance, runtime coordination, cleanup, and correctness. No merge to `main` should happen until real-world testing is approved.

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
- Ascending missile visual.
- Delayed strike phase.
- Last-known target coordinates.
- Last-known air/ground state survives target death before lock.
- Different lock-to-descent delay for air and ground targets.
- Cooldown.

### Missile

- Ascending and descending phases.
- Firework/smoke/flame trail.
- Positional launch/impact flow.
- Explosion damages entities without terrain destruction.

### Audio

Current sound assets include:

```text
static.ogg
launch.ogg
explosion.ogg
```

Current audio works, but future design may add entity-attached loops, distance layers, directional behavior, and Doppler-like pitch changes.

## Performance refactor details

The original implementation used `BlockEntitySearch`, which scanned every block coordinate inside a large volume when each launcher searched for a radar. At radius 128 this could approach roughly 17 million block positions per search, and it repeated across launchers/ticks.

That code has been removed.

The replacement model is:

```text
radar loaded
    -> register coordinate

launcher acquisition cycle
    -> inspect registered radar coordinates only
    -> cheap X/Z squared-distance filtering
    -> resolve only in-range radar block entities
    -> request cached/on-demand radar scans
    -> combine targets
    -> reserve one available target
```

Air-target classification was also changed from a possible 128-block downward loop per check to a heightmap lookup because valid targets are already required to be under open sky.

Unused Fabric template mixins/configuration were removed from the project.

## Real-world test to perform

Recommended sequence:

1. one launcher + one radar + one target;
2. five launchers + four radars + multiple eligible targets;
3. several launchers with only one target, confirming only one launcher engages it;
4. multiple targets, confirming launchers distribute reservations;
5. destroy a radar and verify launchers recover cleanly;
6. destroy/unload a launcher during engagement and verify no permanent reservation remains;
7. test an airborne target and a ground target together, confirming air priority;
8. stress with 10+ launchers/radars and observe world/server tick responsiveness.

Watch specifically for:

- mob/world freezes;
- launchers firing in a synchronized rhythm;
- duplicated engagement of one target;
- stale radar references after breaking/unloading;
- targets never becoming available again after a cancelled engagement;
- altered launch timing or audiovisual sequence.

## Known/intentional constraints

- Radar scans still use Minecraft entity AABB queries; this is expected to be much cheaper than block-volume scanning and is currently cached/shared.
- Missile particle density remains intentionally high because the current visual result was explicitly approved. Revisit only if missile-heavy stress tests show measurable network/client cost.
- PvE is the priority; multiplayer is not currently a design target.
- Existing missile timing/feel should not be casually changed.
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
