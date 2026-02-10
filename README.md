Tribal Trouble
==============
(this section preserved from the [upstream repo](https://github.com/sunenielsen/tribaltrouble))

Tribal Trouble is a realtime strategy game released by Oddlabs in 2004. In 2014 the source was released under GPL2
license, and can be found in this repository.

The source is released "as is", and Oddlabs will not be available for help building it, modifying it or any other kind
of support. Due to the age of the game, it is reasonable to expect there to be some problems on some systems. Oddlabs
has not released updates to the game for years, and do not intend to start updating it now that it is open sourced.

**If** you know how to code Java, configure Gradle, use MySQL, and have a **genuine intention** of actually working on
the game, you can create an issue for detailed questions about the source.

About this fork
---------------

I ([bondolo](https://github.com/bondolo)) have been working on restoring this enjoyable game to working order with
modern Java and OpenGL. This has included updating to more recent LWJGL, JInput and OpenAL, removing the need for 
registration, removing demo mode, removing updater as well as many Java modernizations and cleanups. The build 
system has been migrated from Ant to Gradle for better dependency management and faster builds.

This is a *restoration* fork. While the implementation has been modernized the gameplay intentionally remains as
close as possible to the original game but with better graphics, sound and fluidity. Some essential accessibility
features have been added including UI magnification, high contrast filter, color vision difference correction, team
color editing, and unit/building team color overlays.

If you are looking for multiplayer or new features there is another fork of the game which has focused on that and
has a great community of contributors and players. See **[Tribal Trouble:Resurrected](https://tribaltrouble.org/)**

**Disclaimer**: This fork of Tribal Trouble is a derived work by an opensource effort based on the original Oddlabs
version. It aims to revive the original game and modernize the implementation so that the game may be played long in to
the future. The current maintainers for this edition have no affiliation with Oddlabs.

Building
--------
Clone the repository:

```
git clone https://github.com/bondolo/tribaltrouble.git
cd tribaltrouble
```

Requirements:

- Java SDK 25 or later
- Gradle 9.1+ (or use included wrapper)

Build and run the game:

```
gradle :tt:run
```

Build all modules:

```
gradle build
```

Common tasks:

- `gradle clean` - Clean all build outputs
- `gradle :assets:geometry` - Generate geometry files
- `gradle :assets:textures` - Convert texture files
- `gradle :tt:build` - Build game client
- `gradle :tt:run` - Run the game

Note: Server and servlet modules are excluded due to missing dependencies.

