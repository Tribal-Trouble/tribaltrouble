# Ant to Gradle Task Mapping

This document maps the original Ant build tasks to their Gradle equivalents.

## Root Project

| Ant Task | Gradle Task | Description |
|----------|-------------|-------------|
| `ant clean` | `./gradlew clean` | Clean build directories |
| `ant mrproper` | `./gradlew clean` | Deep clean (Gradle clean is sufficient) |
| `ant compile` | `./gradlew build` | Compile all modules |

## Common Module

| Ant Task | Gradle Task | Description |
|----------|-------------|-------------|
| `ant compile` | `./gradlew :common:build` | Compile common module |
| `ant clean` | `./gradlew :common:clean` | Clean common module |

## Tools Module

| Ant Task | Gradle Task | Description |
|----------|-------------|-------------|
| `ant compile` | `./gradlew :tools:build` | Compile tools module |
| `ant geometry` | `./gradlew :tools:geometry -Pgeometry_file=... -Pressrc=... -Presbuild=...` | Convert geometry files |
| `ant convert` | `./gradlew :tools:convert` | Convert image files |
| `ant createfont` | `./gradlew :tools:createFont -Pfontname=... -Pfontsize=...` | Create font textures |

## TT (Main Game) Module

| Ant Task | Gradle Task | Description |
|----------|-------------|-------------|
| `ant compile` | `./gradlew :tt:build` | Compile game client |
| `ant run` | `./gradlew :tt:run` | Run the game |
| `ant geometry` | `./gradlew :tt:geometry` | Process geometry files |
| `ant textures` | `./gradlew :tt:textures` | Process texture files |
| `ant createfonts` | `./gradlew :tt:createFonts` | Create game fonts |
| `ant revision` | `./gradlew :tt:revision` | Generate revision number |
| `ant generatetextures` | `./gradlew :tt:generateTextures` | Generate procedural textures |
| `ant proceduraltest` | `./gradlew :tt:proceduralTest` | Test procedural generation |

## Server Module

| Ant Task | Gradle Task | Description |
|----------|-------------|-------------|
| `ant compile` | `./gradlew :server:build` | Compile server modules |
| `ant bugreporter` | `./gradlew :server:bugreporter` | Build bug reporter JAR |
| `ant router` | `./gradlew :server:router` | Build router JAR |
| `ant matchmaker` | `./gradlew :server:matchmaker` | Build matchmaking JAR |
| `ant all` | `./gradlew :server:all` | Build all server JARs |

## Servlet Module

| Ant Task | Gradle Task | Description |
|----------|-------------|-------------|
| `ant compile` | `./gradlew :servlet:build` | Compile servlet modules |
| `ant matchservlet` | `./gradlew :servlet:matchservlet` | Build matchmaking servlet WAR |
| `ant regservlet` | `./gradlew :servlet:regservlet` | Build registration servlet WAR |
| `ant graphservlet` | `./gradlew :servlet:graphservlet` | Build graph servlet WAR |

## Truetype Module

| Ant Task | Gradle Task | Description |
|----------|-------------|-------------|
| `ant compile` | `./gradlew :truetype:build` | Compile truetype module |
| `ant run` | `./gradlew :truetype:run` | Run truetype test |

## Common Gradle Commands

```bash
# Build everything
./gradlew build

# Clean and build
./gradlew clean build

# Run the main game
./gradlew :tt:run

# Build specific module
./gradlew :tt:build
./gradlew :server:build

# List all available tasks
./gradlew tasks

# List tasks for specific module
./gradlew :tt:tasks

# Build with parallel execution
./gradlew build --parallel

# Build with info logging
./gradlew build --info
```

## Key Differences

1. **Parallel Execution**: Gradle automatically handles parallel builds based on CPU cores
2. **Incremental Builds**: Gradle only rebuilds what changed
3. **Dependency Management**: Gradle handles module dependencies automatically
4. **Task Dependencies**: Tasks like `run` automatically depend on `geometry`, `textures`, and `revision`
5. **Properties**: Use `-P` flag for properties (e.g., `-Pfontname=Tahoma`)

## Migration Notes

- All Ant `build.xml` files are preserved but no longer used
- Source directories remain unchanged (`classes/` instead of `src/`)
- Build output goes to `build/` directory in each module
- The Gradle wrapper is included, so no need to install Gradle separately
