# Family AI: Animal Bonds

Family AI gives peaceful vanilla animals persistent family behavior.

Baby animals remember their parents, parents remember their children, and families react together when danger appears nearby. Instead of running randomly, a threatened baby tries to reach its mother or father, while alerted parents move to protect it.

## Features

- Persistent mother, father, partner, and child UUID data
- Babies follow their mother first, then their father if needed
- Threatened babies run toward their family instead of fleeing randomly
- Parents enter an alert state when a baby is attacked
- Parents try to stand between the attacker and the baby
- Bonded adult pairs stay near each other while grazing
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

With Mod Menu installed, open the mod list, select **Family AI: Animal Bonds**, and use the config button.

Without Mod Menu, edit:

```text
config/family-ai.json
```

Configurable values include baby follow speed, parent protection speed, family radius, alert duration, warning sound cooldown, and hostile player scan range.

## Debug Command

```mcfunction
/familyai inspect
```

Looks at the animal in your crosshair and prints family data such as parent UUIDs, registered children, alert state, threat UUID, and data version.

## Datapack Support

The AI only applies to entity types in:

```text
family_ai:family_animals
```

Datapacks and other mods can extend this tag to add compatible animals.

## Current Status

This is a beta release. The core family bond system works, but AI balance and species-specific behavior may change in future versions.

Back up important worlds before testing early releases.

## Roadmap

- Sibling recognition
- Baby animals playing with siblings
- Species-specific shelter behavior, such as chicks hiding under mother chickens
- Orphan adoption
- Player trust and distrust
- More herd and flock behaviors

## License

All Rights Reserved.

Official releases may be used for personal gameplay. Redistribution, re-uploading,
fork publishing, and derivative public releases require explicit permission from
the author.
