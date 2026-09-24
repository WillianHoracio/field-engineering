# Project State

Update this file whenever a meaningful feature is completed, removed, or changes direction.

## Working baseline

- Fabric 1.21.1 development environment works.
- Java 21 works.
- Gradle wrapper is pinned to 8.10.2.
- Loom 1.8.13 builds a valid production JAR.
- Production artifact: `build/libs/field-engineering-1.0.0.jar`.
- The mod loads in the real launcher/modpack.

## Implemented

### Radar

- 100-block target range.
- Periodic target scan.
- Open-sky requirement.
- High-value hostile target allow-list.
- Air/ground classification.
- Debug target particles.

### Launcher

- 3x3x2 multiblock footprint.
- Central master block with invisible structural parts.
- Nearest-radar integration.
- Preparation smoke and launch effects.
- Ascending missile visual.
- Delayed strike phase.
- Last-known target coordinates.
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

## Confirmed real-world test

A test with approximately five launchers and four radars against an AirRaid target functioned logically, but produced severe world/server-tick freezes even when launchers were not firing.

The likely cause is the current brute-force nearby block-entity search used to discover radars. With a search radius of 128, a cube can contain about 17 million block positions. Multiplying this by several launchers and repeated execution is predictably too expensive.

This is considered a design/performance defect, not an acceptable cost of the feature.

## Immediate priority

Replace brute-force radar discovery with a registry/cache of active radar positions.

Acceptance goals:

- no scan through millions of block positions;
- radar registration/unregistration follows block entity lifecycle;
- launchers query only known radar entries;
- selected radar is cached when possible;
- invalid/unloaded radar references recover cleanly;
- multiple launchers/radars should remain cheap while idle;
- behavior of the existing missile sequence should remain unchanged.

## Next gameplay work after performance

Potential sequence:

1. radar/launcher performance architecture;
2. air-target priority;
3. target reservation and launcher coordination;
4. ammunition and ownership;
5. interceptors;
6. hostile Illager batteries;
7. tactical Illager AI;
8. siege/game-mode systems and military points of interest.

This list can change according to user design decisions.

## Known design principles

- PvE is the priority; multiplayer is not currently a design target.
- Existing missile timing/feel should not be casually changed; the current launch sequence was explicitly approved.
- World-generation changes should avoid visible seams in already-generated large worlds. Prefer retrofit/event approaches when possible.
- Integrations should avoid hard dependencies on another mod's internals when a robust generic rule is sufficient.
- The user owns gameplay/system design decisions. Code should implement those decisions rather than silently redefining them.
