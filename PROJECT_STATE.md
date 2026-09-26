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
- Long-range missile visibility fix has been merged to `main`.

## Current branch under test

```text
feat/sam-launcher-model-pass
```

This branch begins the visual-model pass for the launcher/radar/missile set. The first step replaces the oversized single-block launcher model with a valid multiblock-rendered static SAM battery that uses the existing 3x3x2 structure.

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
- Radar visual redesign is planned later in this PR.

### Launcher

- 3x3x2 multiblock footprint.
- Central master block plus 17 structural part blocks.
- Structural parts now store their X/Y/Z offset relative to the master.
- Structural parts render dedicated local block models instead of remaining invisible.
- The launcher visual is distributed across the existing 3x3x2 multiblock, avoiding the previous oversized single-block-model limitation.
- Current static SAM concept uses a low armored base, central machinery, four vertical launch cells, bridge supports, and a compact control panel.
- All rendered model geometry stays within normal per-block model bounds.
- Breaking any structural part still destroys the master; offset data is used for direct master lookup with the old local scan retained as fallback.
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
- When missile velocity is near zero, the renderer uses an upward fallback direction so the ceiling trail remains visible.
- `shouldRenderAtSqrDistance` explicitly permits missile rendering up to 1024 blocks.
- Client tracking range remains at the original 10 chunks.
- Explosion damages entities without terrain destruction.
- Missile model redesign is planned after the launcher visual is approved.

### Timing

The strike timing remains unchanged:

```text
launch -> 180 ticks (9.0 s) -> target lock / air descent
                         +22 ticks (1.1 s) -> ground descent
                         +18 ticks (0.9 s) -> impact after descent begins
```

### Audio

Current sound assets include:

```text
static.ogg
launch.ogg
explosion.ogg
```

## Current real-world test

Test the new static SAM launcher model in the real modpack:

1. verify the launcher still places as a 3x3x2 multiblock;
2. confirm all 17 auxiliary blocks render the intended pieces rather than air;
3. verify four vertical launch cells appear in the upper corners;
4. check that there are no floating/disconnected components;
5. confirm no model-too-large warning appears;
6. rotate/place the launcher in different directions and verify the central control panel orientation still follows the master facing;
7. break several different structural parts and confirm the whole launcher still dismantles correctly;
8. fire missiles and verify the visual change does not alter targeting, timing, launch effects, or cleanup;
9. check Sodium/Iris/shader compatibility and obvious z-fighting/seams.

## Known/intentional constraints

- The first launcher pass uses vanilla Minecraft textures as placeholders so geometry can be validated before custom texture work.
- The new launcher is a fixed SAM installation rather than a parked truck/mobile launcher.
- The launcher remains exactly 3x3x2 for gameplay/placement even though future decorative refinements may change perceived proportions.
- Radar and missile model redesigns are still pending in this PR.
- Radar scans still use Minecraft entity AABB queries and shared caching.
- Missile render distance is intentionally bounded at 1024 blocks rather than unlimited.
- PvE is the priority; multiplayer is not currently a design target.
- World-generation changes should avoid visible seams in already-generated large worlds.
- Integrations should avoid hard dependencies on another mod's internals when a robust generic rule is sufficient.
- The user owns gameplay/system design decisions. Code should implement those decisions rather than silently redefining them.

## Next steps in this PR

1. validate and refine the static SAM launcher ingame;
2. redesign the radar in the same visual language;
3. replace/refine the missile model at a scale appropriate to its TNT-like damage;
4. add dedicated textures after geometry is approved;
5. perform final visual/compatibility pass before merge.
