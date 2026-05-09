# Family AI Animal Bonds

Family AI Animal Bonds gives peaceful vanilla animals persistent family behavior.

Baby animals remember their parents, parents remember their children, and families react together when danger appears nearby. Instead of running randomly, a threatened baby tries to reach its mother or father, while alerted parents move to protect it.

## Features

- Persistent mother, father, partner, and child UUID data
- Persistent sibling bond data
- Persistent per-player reputation data
- Babies follow their mother first, then their father if needed
- Threatened babies run toward their family instead of fleeing randomly
- Parents enter an alert state when a baby is attacked
- Parents try to stand between the attacker and the baby
- Bonded adult pairs stay near each other while grazing
- Babies can play with recognized siblings
- Feeding raises trust, attacking lowers trust, and nearby herd members learn from it
- Config file at `config/family-ai.json`
- Optional Mod Menu config screen
- `/familyai inspect` debug command
- Datapack tag support through `family_ai:family_animals`

## Compatibility

- Minecraft: `1.21.11`
- Loader: Fabric
- Required: Fabric API `0.141.3+1.21.11` or newer for `1.21.11`
- Optional: Mod Menu `17.0.0+`

## In-Game Config

With Mod Menu installed, open the mod list, select **Family AI Animal Bonds**, and use the config button.
The config is grouped by categories (family core, protection, reputation, spawns, sibling play) for faster navigation.

Without Mod Menu, edit:

```text
config/family-ai.json
```

Configurable values include follow and protection behavior, alert windows, sibling play tuning, and the full reputation system (gain/loss, hostility threshold, and herd gossip).

## Debug Command

```mcfunction
/familyai inspect
```

Looks at the animal in your crosshair and prints family data, sibling count, your current reputation with that animal, and NBT data version.

## Datapack Support

The family AI behavior system only applies to entity types in:

```text
family_ai:family_animals
```

Datapacks and other mods can extend this tag to add compatible animals.

## Project Links

- Changelog: `CHANGELOG.md`
- Contributing: `CONTRIBUTING.md`
- Issues: use GitHub issue templates (bug report / feature request)

## Current Status

This is build `1.1.0`. The family core is stable, with sibling play and player reputation now included.

Back up important worlds before testing early releases.

## Roadmap

- Species-specific shelter behavior, such as chicks hiding under mother chickens
- Orphan adoption
- More herd and flock behaviors

## License

All Rights Reserved  
Copyright (c) 2026 Luiz Otávio Marques
