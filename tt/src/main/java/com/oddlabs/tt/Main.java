package com.oddlabs.tt;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

	public static void fail(@NonNull Throwable t) {
		try {
            logger.log(Level.SEVERE, "Critical Failure", t);
			if (Display.isCreated())
				Display.destroy();
			while (t.getCause() != null) {
                t = t.getCause();
            }
			ResourceBundle bundle = ResourceBundle.getBundle(Main.class.getName());
			String error = Utils.getBundleString(bundle, "error");
			String error_msg = Utils.getBundleString(bundle, "error_message", t.toString());
			Sys.alert(error, error_msg);
		} finally {
			shutdown();
		}
	}

	public static void shutdown() {
        logger.info("Exiting");
		System.exit(0);
	}

    static void main(@NonNull String... args) {
		try {
            logger.info("Starting game....");
			Renderer.runGame(args);
		} catch (Throwable t) {
			fail(t);
		} finally {
			shutdown();
		}
	}
}
