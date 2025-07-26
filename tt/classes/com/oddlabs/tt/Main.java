package com.oddlabs.tt;

import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.util.Utils;

import java.util.ResourceBundle;

import com.oddlabs.tt.render.Display;

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
            Display.create();
			Renderer.runGame(args);
		} catch (Throwable t) {
			fail(t);
		} finally {
			shutdown();
		}
	}
}
