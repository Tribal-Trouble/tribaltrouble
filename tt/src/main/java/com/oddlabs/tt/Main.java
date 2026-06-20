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
    private static final boolean DEBUG = Boolean.getBoolean("com.oddlabs.tt.developer");
    private static final ResourceBundle bundle = ResourceBundle.getBundle(Main.class.getName());

    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    public static void fail(@NonNull Throwable t) {
        logger.log(Level.SEVERE, "Critical Failure", t);

        if (!DEBUG) {
            while (t.getCause() != null) {
                t = t.getCause();
            }
            String error = i18n("error");
            String error_msg;
            try {
                error_msg = i18n("error_message", t.toString());
            } catch (IllegalArgumentException e) {
                // Fallback if message formatting fails (e.g. quotes in the exception message)
                error_msg = "Error: " + t;
            }
            logger.log(Level.SEVERE, error + ": " + error_msg);
            TinyFileDialogs.tinyfd_messageBox(error, error_msg.replace("\"", "\\\""), "ok", "error", 1);
        }
    }

    public static void shutdown(int status) {
        Renderer.getRenderer().close();
        logger.info("Exiting");
        System.exit(status);
    }

    static void main(@NonNull String @NonNull... args) {
        logger.info("DEBUG mode = " + DEBUG);
        String os_name = System.getProperty("os.name");
        logger.info("os.name = '" + os_name + "'");
        String os_arch = System.getProperty("os.arch");
        logger.info("os.arch = '" + os_arch + "'");
        String os_version = System.getProperty("os.version");
        logger.info("os.version = '" + os_version + "'");
        String java_version = System.getProperty("java.version");
        logger.info("java.version = '" + java_version + "'");
        String java_vendor = System.getProperty("java.vendor");
        logger.info("java.vendor = '" + java_vendor + "'");
        long total_mem = Runtime.getRuntime().maxMemory();
        logger.info("maxMemory = '" + total_mem + "'");

        int status = 1;
        try {
            logger.info("Starting game....");
            Renderer.getRenderer().run(args);
            status = 0;
        } catch (Throwable t) {
            fail(t);
        } finally {
            shutdown(status);
        }
    }
}
