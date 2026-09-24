# Development Guidelines

These rules exist to keep the mod scalable as systems become more complex.

## 1. Performance is a design requirement

Performance must be considered before implementation, not only after lag appears.

Before writing a tick-driven feature, estimate:

```text
work per execution
x executions per second
x number of active instances
x worst plausible world scale
```

A solution that works with one block/entity but scales badly with five or fifty instances is not acceptable.

### Avoid

- scanning every block in a large 3D radius;
- scanning large world areas every tick;
- expensive pathfinding or entity searches every tick without need;
- recomputing stable relationships that can be cached;
- each instance independently rediscovering the same global information;
- hidden O(volume) work inside apparently simple helper methods;
- unbounded collections or permanent references to unloaded world objects.

### Prefer

- registries/indexes of known relevant objects;
- event/lifecycle-driven registration and invalidation;
- caching with explicit invalidation;
- bounded spatial/entity queries;
- scan intervals instead of per-tick scans when reaction time allows;
- state machines that do work only in relevant states;
- UUID/position references instead of retaining unsafe entity/block-entity objects indefinitely;
- server/client separation so authoritative gameplay logic is not duplicated unnecessarily.

## 2. Preserve approved behavior

When optimizing or refactoring, do not alter gameplay timing, effects, targeting rules, or other approved behavior unless the requested change requires it.

For the current missile system, performance changes should preserve the established preparation, ascent, wait, lock, descent, sound, and impact sequence.

## 3. Centralize rules

If the same gameplay rule is needed in more than one place, put it in one rules/service component.

Examples:

- target eligibility;
- air/ground classification;
- target reservation;
- radar availability;
- ammunition eligibility;
- engagement priority.

Avoid copy/pasted rule logic in multiple block entities.

## 4. Prefer explicit state

For systems with timing or asynchronous behavior, use explicit state/fields rather than chains of loosely related conditions.

A future complex launcher may benefit from states such as:

```text
IDLE
ACQUIRING
PREPARING
ASCENDING
WAITING_FOR_LOCK
DESCENDING
COOLDOWN
```

Only introduce state complexity when it improves correctness or clarity.

## 5. Design for many instances

When adding a system, assume the player may build many of them.

At minimum consider:

- 1 instance;
- 10 instances;
- 50 instances;
- several instances in loaded neighboring chunks.

Shared managers should be scoped appropriately by server level/dimension and should clean up entries when chunks/entities unload.

## 6. Mod compatibility

Prefer generic Minecraft/Fabric behavior over hard coupling to another mod unless integration requires that API.

Example: current air-target classification is based on height above ground rather than identifying an AirRaid-specific vehicle. This keeps the targeting system useful with other mods.

## 7. Documentation workflow

Before changing code:

1. read `PROJECT_STATE.md`;
2. read the relevant section of `ARCHITECTURE.md`;
3. read this file when choosing implementation strategy;
4. inspect only the relevant source files;
5. expand inspection only if dependencies require it.

After a meaningful change:

- update `PROJECT_STATE.md` if implementation status changed;
- update `ARCHITECTURE.md` if responsibilities/data flow changed;
- update this file only when a durable engineering principle changes.

The documentation is intended to reduce repository-wide rescans and keep future agent work focused.

## 8. Validation before commit

For code changes, when tools/environment allow:

```powershell
.\gradlew.bat build
```

For performance-sensitive changes, successful compilation is not enough. Also reason about operation count and test with multiple active instances in the real modpack when practical.

## 9. Commit scope

Keep commits focused. A performance refactor should not also redesign gameplay unless both are inseparable. Commit messages should describe the behavior or architecture changed, not generic wording such as `update code`.
