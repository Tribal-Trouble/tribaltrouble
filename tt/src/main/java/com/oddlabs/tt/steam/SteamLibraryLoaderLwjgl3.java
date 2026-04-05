package com.oddlabs.tt.steam;

import com.codedisaster.steamworks.SteamLibraryLoader;
import org.lwjgl.system.Library;
import org.lwjgl.system.Platform;

import java.util.logging.Logger;

public final class SteamLibraryLoaderLwjgl3 implements SteamLibraryLoader {
    private static final Logger logger = Logger.getLogger(SteamLibraryLoaderLwjgl3.class.getName());

    @Override
    public void setLibraryPath(String libraryPath) {
        System.setProperty("org.lwjgl.librarypath", libraryPath);
    }

    @Override
    public boolean loadLibrary(String libraryName) {
        if (Platform.get() == Platform.WINDOWS && Platform.getArchitecture() == Platform.Architecture.X64) {
            libraryName = libraryName + "64";
        }

        try {
            Library.loadSystem("com.codedisaster.steamworks", libraryName);
            return true;
        } catch (Throwable t) {
            logger.warning("Failed to load Steam library: " + libraryName + " - " + t.getMessage());
            return false;
        }
    }
}
