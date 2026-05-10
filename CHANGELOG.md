# Changelog

All notable changes to this project are documented here.

## [1.2.0] - 2026-05-10

### Added
- Full AI state machine for family animals:
  - `IDLE`, `GRAZE`, `FOLLOW_PARENT`, `FOLLOW_HERD`, `REGROUP`, `ALERT`, `FLEE`,
    `PROTECT_CHILD`, `SEARCH_PARENT`, `LOST_CHILD`, `FOLLOW_TEMP_ADULT`,
    `STUCK_RECOVERY`, `PANIC_COOLDOWN`.
- Lightweight animal memory and personality data persisted in NBT:
  - state, threat timestamp, panic cooldown, path fail counters, leader/temp-guardian UUID,
    anti-recursion natural-spawn marker, and trait values.
- Advanced herd behavior loop with:
  - cohesion, separation, alignment, regrouping, temporary leader resolution, and panic motion.
- Optional HUD overlay with modes:
  - `OFF`, `SIMPLE`, `DETAILED`, `DEBUG`.
- New guide flow:
  - first-join welcome messages and guide item delivery logic (`/familyai guide` to request again).
- Expanded command set:
  - `/familyai status`
  - `/familyai inspect` and `/familyai animalinfo`
  - `/familyai herdinfo`
  - `/familyai reload`
  - `/familyai debug on|off`
  - `/familyai hud <off|simple|detailed|debug>`
  - `/familyai guide`
- Mod Menu config redesign into visual categories with emoji labels.

### Changed
- Reworked config model for 1.2.0 with safer defaults and broader control of AI, danger, HUD, debug, and performance.
- Child behavior now prefers mother, falls back to father, and then tries temporary adult guardians.
- Adult behavior now uses herd context before moving, reducing random wandering and single-point piling.
- Release/package structure updated for new official release:
  - `release-package/versions/1.2.0/{artifacts,assets,docs,CHECKSUMS.txt,RELEASE-NOTES.txt}`

### Fixed
- Natural family generation anti-cascade protections improved:
  - persistent `NaturalSpawnProcessed` marker
  - anchor election per local group
  - chunk cooldown and local cap enforcement
- Added stronger null/invalid safety around UUID-linked family lookups and state transitions.
- Reduced path retry loops by adding path failure counters, recalc cooldown, and unstuck recovery steps.

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
- Mod Menu config UI reorganized into cleaner categories.

### Fixed
- Fixed recursive natural family spawn cascades that could generate oversized groups.

## [1.0.0] - 2026-05-08

### Added
- Persistent family bond core:
  - Mother/father UUID tracking
  - Parent-child registration
  - Partner cohesion behavior
- Baby follow and refuge behavior.
- Parent protection behavior in alert state.
- Config file support (`config/family-ai.json`).
- Mod Menu config integration.
- `/familyai inspect` command.
