package com.oddlabs.tt;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.util.Utils;

import java.util.ResourceBundle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.oddlabs.tt.render.Display;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.updater.UpdateInfo;

import org.lwjgl.system.Platform;

public final strictfp class Main {
	public final static void fail(Throwable t) {
		try {
			t.printStackTrace();
			if (Display.isCreated())
				Display.destroy();
			while (t.getCause() != null)
				t = t.getCause();
			ResourceBundle bundle = ResourceBundle.getBundle(Main.class.getName());
			String error = Utils.getBundleString(bundle, "error");
			String error_msg = Utils.getBundleString(bundle, "error_message", new Object[]{t.toString(), Globals.SUPPORT_ADDRESS});
			System.out.println(error + " : " + error_msg);
		} finally {
			shutdown();
		}
	}

	public final static void shutdown() {
		System.exit(0);
	}

	public final static void main(String[] args) {
		try {
			System.out.println("Starting game....");
			System.out.flush();
			Main.class.getClassLoader().setDefaultAssertionStatus(true);
			// Parse command line arguments
			boolean grab_frames = parseGrabFrames(args);
			boolean eventload = parseEventLoad(args);
			boolean zipped = parseZipped(args);
			boolean silent = parseSilent(args);

			// Initialize settings (so they can be used by display and renderer)
			Settings settings = initializeSettings();
			UpdateInfo update_info = parseBootstrapUpdateInfo(args, settings);


			// Create display and run game
			Display.create();
			Renderer.runGame(grab_frames, eventload, zipped, silent, update_info);
		} catch (Throwable t) {
			fail(t);
		} finally {
			shutdown();
		}
	}


	private static final Settings initializeSettings() {
		System.out.println("Initializing settings...");
		String platform_dir;
		if (Platform.get() == Platform.MACOSX) {
			platform_dir = "Library/Application Support" + File.separator;
		} else if (Platform.get() == Platform.LINUX) {
			platform_dir = ".";
		} else {
			platform_dir = "";
		}
		String game_dir_path = System.getProperty("user.home") + File.separator + platform_dir + Globals.GAME_NAME;
		File game_dir = new File(game_dir_path);
		Settings settings = new Settings();
		game_dir.mkdirs();

		// fetch initial settings
		settings.load(game_dir);
		Settings.setSettings(settings);
		return settings;
	}


	private static boolean parseGrabFrames(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--grabframes")) {
				return true;
			}
		}
		return false;
	}

	private static boolean parseEventLoad(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--eventload")) {
				return true;
			}
		}
		return false;
	}

	private static boolean parseZipped(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--eventload")) {
				if (i + 1 < args.length && args[i + 1].equals("zipped")) {
					return true;
				}
				else if( i + 1 < args.length && args[i + 1].equals("normal")) {
					return false;
				} else {
					throw new RuntimeException("Unknown argument for --eventload: " + args[i + 1]);
				}
			}
		}
		return false;
	}

	private static boolean parseSilent(String[] args) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--silent")) {
				return true;
			}
		}
		return false;
	}

	private static UpdateInfo parseBootstrapUpdateInfo(String[] args, Settings settings) {
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("--bootstrap")) {
				String java_cmd = args[++i];
				settings.load(Utils.getInstallDir());
				String classpath = args[++i];
				File data_dir = new File(args[++i]);
				return new UpdateInfo(java_cmd, classpath, data_dir);
			}
		}
		return null;
	}
}
