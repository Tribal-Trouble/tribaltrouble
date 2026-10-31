# assets/ notes for agents

Read the root [AGENTS.md](../AGENTS.md) first.

## No generated art or sound

Every texture, model, icon, font, and sound in this game is made by a person. Do not
generate one, and do not run an existing one through a model to upscale, restyle, denoise,
or clean it up. If a feature needs an asset that does not exist, say so and stop rather than
filling the gap. See the [creative work policy](../AGENTS.md#policy-creative-work-stays-human).

Assets also stay in this repository. Do not upload them anywhere, including to scratch
repos, image hosts, or conversion services.

## What this module does

`assets` holds art sources and the Gradle tasks that convert them into the binary formats
the client loads at runtime. The conversion itself is deterministic tooling, not creative
work, so regenerating output is fine.

- `./gradlew assets:geometry` rebuilds binary geometry
- `./gradlew assets:textures` converts texture sources

Texture conversion shells out to `basisu` when it is on `PATH` (or via `-PbasisuPath=`).
Prefer regenerating derived files with these tasks over committing hand-built binaries.

The converters themselves live in `../tools` (`com.oddlabs.converter`, `fontutil`,
`imageutil`).
