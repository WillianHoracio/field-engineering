# Field Engineering

Field Engineering is a Fabric 1.21.1 mod focused on military engineering, radar and missile systems, tactical AI, and immersive combat mechanics.

## Development environment

- Minecraft: 1.21.1
- Fabric Loader: 0.19.5
- Fabric API: 0.116.17+1.21.1
- Fabric Loom: 1.8.13
- Java: 21
- Gradle: 8.10.2

Build on Windows:

```powershell
.\gradlew.bat build
```

Production JAR:

```text
build/libs/field-engineering-1.0.0.jar
```

## Before changing code

To avoid repeatedly scanning the whole repository, use this order:

1. Read `PROJECT_STATE.md` for the current implementation and immediate work.
2. Read `ARCHITECTURE.md` for systems, responsibilities, flows, and file ownership.
3. Read `DEVELOPMENT_GUIDELINES.md` before choosing an implementation, especially for tick/performance-sensitive systems.
4. Open only the source files relevant to the requested change.

When a feature changes architecture, gameplay rules, or project state, update the corresponding documentation in the same change.

## Current prototype

The prototype currently contains a radar, a multiblock missile launcher, missile entities, targeting rules, sounds, particles, and a basic radar-to-launcher engagement flow.

The next critical task is performance work: the current launcher can search for nearby radar block entities through a large brute-force world scan. This must be replaced by a bounded registry/cache approach before expanding the number of active systems.

## Project direction

The project may expand into tactical PvE systems such as hostile Illager installations, improved combat AI, anti-missile defenses, artillery, military points of interest, and siege-oriented gameplay. Features should remain modular so the engineering systems can evolve independently from higher-level game modes.

## License

See `LICENSE`.
