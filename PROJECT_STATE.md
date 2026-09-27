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

- Structure now reserves a 3x3x3 volume so the launch canisters can read as tall vertical tubes instead of a short two-level machine.
- Four canisters are grouped visually toward the center of the battery.
- Each canister spans two upper multiblock layers and uses dark reinforcing bands plus a capped top.
- Central/cardinal upper pieces are compact support/electronics structures rather than broad horizontal slabs.
- The lower layer remains a heavy fixed installation base.
- Structural parts still resolve directly back to the master through encoded offsets and keep a local fallback lookup.

### Missile

- The missile entity now renders the mod's `aa_missile` item rather than a vanilla firework rocket.
- The item model is now a compact 3D interceptor-style missile with a pale body, dark tail/fins, and a marked nose.
- Render scale is reduced from the temporary 2x firework scale to 1.6x for the dedicated model.
- Existing long-range render distance, trail, particles, timing, impact precision, and TNT-like entity damage remain unchanged.

## Real-world test to perform

1. verify the launcher now reads immediately as a fixed anti-air missile battery;
2. verify the four canisters look grouped rather than scattered across the 3x3 footprint;
3. verify the extra height does not create detached/floating geometry;
4. verify placement correctly requires the new third layer of free space;
5. break parts from all three layers and verify the master/whole structure still dismantles;
6. verify the custom missile model is visible during ascent, ceiling wait, and descent;
7. verify missile visuals remain readable at long range and the existing trail still follows it;
8. verify targeting, timing, cooldown, impact, damage, and radar behavior are unchanged.

## Known/intentional constraints

- The launcher currently uses vanilla textures as placeholders while geometry is validated.
- Missile item geometry uses vanilla block textures for the same reason.
- PvE remains the priority; multiplayer is not currently a design target.
- Gameplay/system behavior should not be redefined during this visual pass unless explicitly requested.

## Next visual work after launcher approval

1. final launcher proportion/texturing pass;
2. radar redesign in the same visual language;
3. missile orientation/polish if the dedicated item model needs refinement in flight;
4. final Sodium/Iris/shader compatibility check before merge.
