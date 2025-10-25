package com.oddlabs.tt;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.util.Utils;
import org.lwjgl.Sys;
import org.lwjgl.opengl.Display;

import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {
	public static void fail(Throwable t) {
		try {
			Logger.getLogger(Main.class.getName()).log(Level.SEVERE, "Critical Failure", t);
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
        Logger.getLogger(Main.class.getName()).info("Exiting");
		System.exit(0);
	}

	public static void main(String[] args) {
		try {
			Logger.getLogger(Main.class.getName()).info("Starting game....");
			System.setProperty("org.lwjgl.util.Debug", "true");
            Logger.getLogger(Main.class.getName()).log(Level.CONFIG, "System.getProperty(\"java.library.path\") = {0}", System.getProperty("java.library.path"));
			Main.class.getClassLoader().setDefaultAssertionStatus(true);
			Renderer.runGame(args);
		} catch (Throwable t) {
			fail(t);
		} finally {
			shutdown();
		}
	}
}
