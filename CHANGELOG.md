# Changelog

All notable changes to this project will be documented in this file.

## [1.1.0] - 2026-05-09

### Added
- Persistent trust system per player UUID (`-100` to `+100`) with states:
  - Trusted (`>= 40`)
  - Neutral (`-19 to 39`)
  - Wary (`-20 to -59`)
  - Hostile (`<= -60`)
- Reputation events:
  - Feed: `+4` (cooldown per player)
  - Breeding assist: `+8`
  - Defend herd: `+10`
  - Hit adult: `-15`
  - Hit baby: `-40`
  - Kill family member: `-80`
  - Sprinting with weapon near babies: `-5` periodic
- Herd gossip propagation with configurable spread factor and radius.
- Reputation decay over time to avoid permanent punishment.
- Sibling recognition persisted in NBT.
- Sibling play behavior goal for babies.
- Natural family herd generation with safety caps and chunk cooldown.

### Changed
- `/familyai inspect` now shows player reputation score and state.
- Mod Menu config now exposes reputation, gossip, spawn, and sibling controls.
- Mod Menu config UI reorganized into clean categories to reduce screen clutter.
- Data version incremented for new NBT fields.

## [1.0.0] - 2026-05-08

### Added
- Persistent family bond core:
  - Mother/father UUID tracking
  - Parent child registration
  - Partner cohesion behavior
- Baby follow and refuge behavior.
- Parent protection behavior in alert state.
- Config file support (`config/family-ai.json`).
- Mod Menu config integration.
- `/familyai inspect` command.
