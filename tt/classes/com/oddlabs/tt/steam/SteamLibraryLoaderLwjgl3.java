package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamLibraryLoader;

import org.lwjgl.system.Library;
import org.lwjgl.system.Platform;

/**
 * LWJGL3-based Steam library loader implementation. Based on steamworks4j-lwjgl3 loader from
 * https://github.com/code-disaster/steamworks4j
 */
public class SteamLibraryLoaderLwjgl3 implements SteamLibraryLoader {

    private static final boolean DEBUG = false;

    private void debug(String message) {
        if (DEBUG) {
            System.out.println("[SteamLoader] " + message);
        }
    }

    @Override
    public void setLibraryPath(String libraryPath) {
        System.setProperty("org.lwjgl.librarypath", libraryPath);
    }

    @Override
    public boolean loadLibrary(String libraryName) {
        debug("Attempting to load library: " + libraryName);
        Platform os = Platform.get();
        Platform.Architecture arch = Platform.getArchitecture();
        debug("Platform: " + os + ", Architecture: " + arch);

        // On Windows 64-bit, Steam libraries are suffixed with "64".
        if (os == Platform.WINDOWS && arch == Platform.Architecture.X64) {
            libraryName = libraryName + "64";
        }

        debug("Final library name: " + libraryName);

        // Let LWJGL3 do its magic
        try {
            Library.loadSystem("com.codedisaster.steamworks", libraryName);
            debug("Successfully loaded: " + libraryName);
        } catch (Throwable t) {
            System.err.println("Failed to load library: " + libraryName);
            t.printStackTrace();
            return false;
        }

        return true;
    }
}
