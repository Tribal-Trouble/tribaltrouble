package com.oddlabs.tt;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
    private static final Logger logger = Logger.getLogger(Main.class.getName());

	public static void fail(@NonNull Throwable t) {
        logger.log(Level.SEVERE, "Critical Failure", t);

        if (Renderer.getRenderer().getWindow() != null)
            Renderer.getRenderer().getWindow().close();

        if (!Boolean.getBoolean("com.oddlabs.tt.developer")) {
            while (t.getCause() != null) {
                t = t.getCause();
            }
            ResourceBundle bundle = ResourceBundle.getBundle(Main.class.getName());
            String error = Utils.getBundleString(bundle, "error");
            String error_msg;
            try {
                error_msg = Utils.getBundleString(bundle, "error_message", t.toString());
            } catch (IllegalArgumentException e) {
                // Fallback if message formatting fails (e.g. quotes in exception message)
                error_msg = "Error: " + t.toString();
            }
            logger.log(Level.SEVERE, error + ": " + error_msg);
            TinyFileDialogs.tinyfd_messageBox(error, error_msg.replace("\"", "\\\""), "ok", "error", true);
        }
	}

	public static void shutdown() {
        logger.info("Exiting");
		System.exit(0);
	}

    static void main(@NonNull String @NonNull ... args) {
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
