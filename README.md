# Family AI Animal Bonds

Family AI Animal Bonds upgrades peaceful vanilla animals with persistent family behavior, herd coordination, and safer danger reactions.

## Version

Current stable release: `1.2.0`

## Core Features

- Persistent family data in NBT:
  - mother, father, partner, children, siblings
- Parent-priority baby logic:
  - baby follows mother first, then father, then temporary adult guardian
- Family protection:
  - parents enter alert/protect behavior when babies are threatened
- Herd intelligence:
  - cohesion, separation, alignment, regrouping, temporary herd leadership
- Threat reaction:
  - babies and adults avoid panic randomness and prefer family-safe movement
- Anti-stuck behavior:
  - path retry cooldown, fail counters, and unstuck recovery logic
- Player reputation:
  - trust/wary/hostile states with herd gossip and decay over time
- Optional HUD:
  - `OFF`, `SIMPLE`, `DETAILED`, `DEBUG`
- Optional chat notices and first-join guide flow
- Mod Menu integration with organized categories

## Commands

```mcfunction
/familyai status
/familyai inspect
/familyai animalinfo
/familyai herdinfo
/familyai reload
/familyai debug on
/familyai debug off
/familyai hud off
/familyai hud simple
/familyai hud detailed
/familyai hud debug
/familyai guide
```

## Compatibility

- Minecraft: `1.21.11`
- Loader: Fabric
- Required: Fabric API `0.141.3+1.21.11` or newer
- Optional: Mod Menu `17.0.0+`

## Config

Without Mod Menu, edit:

```text
config/family-ai.json
```

Main 1.2.0 categories:

- Animal AI
- Family System
- Herd
- Danger Reaction
- Personality & Memory
- Pathfinding & Movement
- HUD & Alerts
- Debug & Logs
- Performance

## Datapack Support

Family behavior applies to entity types listed in:

```text
family_ai:family_animals
```

## License

All Rights Reserved  
Copyright (c) 2026 Luiz Otavio Marques
