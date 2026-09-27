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

This branch is the visual-model pass for the launcher/radar/missile set. The launcher is being pushed toward a fixed SAM-battery silhouette rather than a generic industrial machine.

## Current visual changes under test

### Launcher

- Structure reserves a 3x3x3 volume so the launch canisters read as tall vertical tubes.
- The four canisters are now packed together in a single centered 2x2 cluster above the middle of the battery rather than being spread across four outer multiblock cells.
- The centered cluster spans the two upper layers and keeps consistent spacing/alignment between all four tubes.
- Cardinal upper pieces remain compact support/electronics structures; diagonal upper pieces are visually empty so the launcher silhouette is dominated by the central canister pack.
- The lower layer remains a heavy fixed installation base.
- Structural parts still resolve directly back to the master through encoded offsets and keep a local fallback lookup.

### Missile

- The missile entity renders the mod's `aa_missile` item rather than a vanilla firework rocket.
- The item model has explicit volumetric body sections and four physical tail fins so it reads as a 3D object from every angle.
- The renderer no longer uses `ThrownItemRenderer` billboard orientation; it renders the item model directly and aligns the missile's local upward axis to its actual velocity vector.
- Ascent therefore points the nose upward, while descent automatically rotates the nose toward the ground.
- When velocity is nearly zero at the world ceiling, the renderer keeps the approved upward orientation.
- Render scale remains 1.6x for long-range readability.
- Existing long-range render distance, trail, particles, timing, impact precision, and TNT-like entity damage remain unchanged.

## Real-world test to perform

1. verify the launcher reads immediately as a fixed anti-air missile battery;
2. verify all four canisters are aligned, centered, and visibly closer together;
3. verify the extra height does not create detached/floating geometry;
4. verify placement still correctly requires the third layer of free space;
5. break parts from all three layers and verify the master/whole structure still dismantles;
6. verify the custom missile is visibly three-dimensional from side and diagonal views;
7. verify ascent points the missile nose upward;
8. verify descent flips the missile so the nose points toward the target/ground;
9. verify the ceiling wait keeps a stable upward orientation;
10. verify the existing long-range trail still follows the missile and targeting/timing/cooldown/impact/damage are unchanged.

## Known/intentional constraints

- The launcher currently uses vanilla textures as placeholders while geometry is validated.
- Missile item geometry uses vanilla block textures for the same reason.
- PvE remains the priority; multiplayer is not currently a design target.
- Gameplay/system behavior should not be redefined during this visual pass unless explicitly requested.

## Next visual work after launcher approval

1. final launcher proportion/texturing pass;
2. radar redesign in the same visual language;
3. missile scale/texturing polish if needed after orientation testing;
4. final Sodium/Iris/shader compatibility check before merge.
