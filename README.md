Tribal Trouble
==============
(this section preserved from the [upstream repo](https://github.com/sunenielsen/tribaltrouble))

Tribal Trouble is a realtime strategy game released by Oddlabs in 2004. In 2014 the source was released under GPL2 license, and can be found in this repository.

The source is released "as is", and Oddlabs will not be available for help building it, modifying it or any other kind of support. Due to the age of the game, it is reasonable to expect there to be some problems on some systems. Oddlabs has not released updates to the game for years, and do not intend to start updating it now that it is open sourced.

**If** you know how to code Java, configure Gradle, use MySQL, and have a **genuine intention** of actually working on the game, you can create an issue for detailed questions about the source.

About this fork
---------------

I have been working to restore this enjoyable game to working order with modern Java. This has included updating to more recent LWJGL, JInput and OpenAL, removing the need for registration, removing demo mode, removing updater as well as many Java modernizations and cleanups. The build system has been migrated from Ant to Gradle for better dependency management and faster builds.

Building
--------
Clone the repository:
```
git clone https://github.com/bondolo/tribaltrouble.git
cd tribaltrouble
```

Requirements:
- Java SDK 8 or later
- Gradle 9.1+ (or use included wrapper)

Build and run the game:
```
gradle :tt:run
```

Build all modules:
```
gradle build
```

The Gradle build includes:
- Parallel execution for faster builds
- Configuration cache for quick subsequent builds
- Build cache to reuse outputs across clean builds
- Incremental compilation
- Automatic resource generation (geometry, textures)

Common tasks:
- `gradle clean` - Clean all build outputs
- `gradle :tt:build` - Build game client
- `gradle :tt:run` - Run the game
- `gradle :tt:geometry` - Generate geometry files
- `gradle :tt:textures` - Convert texture files

Note: Server and servlet modules are excluded due to missing dependencies.

Setting up a server is a lot more complex, and not something we have done in many years. It will take some work to get it working, but try looking at the server folder and see if you can figure it out. At the very least, you should know a bit about setting up a MySQL server.
