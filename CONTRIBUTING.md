# Contributing

Thanks for contributing to Family Animal Bonds.

## Requirements
- Java 21+
- Git

## Local build

```powershell
./gradlew.bat clean build
```

Output JAR:
- `build/libs/family-ai-<version>.jar`

## Coding notes
- Keep behavior configurable through `FamilyAiConfig`.
- Preserve NBT compatibility when adding new persistent data.
- Avoid introducing new goals that conflict with existing family priorities.

## Pull requests
- Keep PRs focused by feature/fix.
- Update `CHANGELOG.md` for user-visible behavior changes.
- Include test notes in the PR description.
