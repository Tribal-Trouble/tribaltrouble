package com.oddlabs.tt.global;

import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.LocalInput;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Properties;
import java.util.logging.Logger;

public final class Settings implements Serializable {
	@Serial
	private final static long serialVersionUID = 1L;

	private static Settings settings;

	// event logging
	private static final Logger logger = Logger.getLogger(Settings.class.getName());
	public transient @NonNull Path last_event_log_dir = Path.of("");
	public int last_revision = -1;
	public boolean crashed = false;

	// network
	public String username = "";
	public String pw_digest = "";
	public boolean remember_login = false;

	public int graphic_detail = Globals.DETAIL_NORMAL;

	// sound
	public boolean play_music = true;
	public boolean play_sfx = true;
	public float music_gain = .5f;
	public float sound_gain = 1f;

	// language
	public @NonNull String language = "default";

	// window
	public int view_width = 800;
	public int view_height = 600;
	public int view_freq = 75;

	public int new_view_width = view_width;
	public int new_view_height = view_height;
	public int new_view_freq = view_freq;

	public boolean fullscreen = true;
	public final boolean vsync = true;
//	public int view_bpp = 32;

	// control
	public boolean invert_camera_pitch = false;
	public boolean aggressive_units = false;
	public boolean use_native_cursor = true;

	public float mapmode_delay = .5f;
	public float tooltip_delay = .5f;
	private final boolean developer_mode = Boolean.getBoolean("com.oddlabs.tt.developer");
	public boolean has_native_campaign = false;

	public final boolean save_event_log = true;
	public final boolean fullscreen_depth_workaround = true;
	public boolean generate_dummy_worlds = false;
	public boolean first_run = true;

	public boolean warning_no_sound = true;

	public final boolean hide_multiplayer = false;

	public final int frame_grab_milliseconds_per_frame = 40;

	public static void setSettings(Settings new_settings) {
		settings = new_settings;
	}

	public static Settings getSettings() {
		return settings;
	}

	public boolean inDeveloperMode() {
		return developer_mode;
	}

	public void save() {
		if (LocalEventQueue.getQueue().getDeterministic().isPlayback())
			return;
		Settings defaults = new Settings();
		Properties props = new Properties();

		setProperty(props, "last_event_log_dir", last_event_log_dir, defaults.last_event_log_dir);
		setProperty(props, "last_revision", last_revision, defaults.last_revision);
		setProperty(props, "crashed", crashed, defaults.crashed);
		setProperty(props, "username", username, defaults.username);
		setProperty(props, "pw_digest", pw_digest, defaults.pw_digest);
		setProperty(props, "remember_login", remember_login, defaults.remember_login);
		setProperty(props, "graphic_detail", graphic_detail, defaults.graphic_detail);
		setProperty(props, "play_music", play_music, defaults.play_music);
		setProperty(props, "play_sfx", play_sfx, defaults.play_sfx);
		setProperty(props, "music_gain", music_gain, defaults.music_gain);
		setProperty(props, "sound_gain", sound_gain, defaults.sound_gain);
		setProperty(props, "language", language, defaults.language);
		setProperty(props, "view_width", view_width, defaults.view_width);
		setProperty(props, "view_height", view_height, defaults.view_height);
		setProperty(props, "view_freq", view_freq, defaults.view_freq);
		setProperty(props, "new_view_width", new_view_width, defaults.new_view_width);
		setProperty(props, "new_view_height", new_view_height, defaults.new_view_height);
		setProperty(props, "new_view_freq", new_view_freq, defaults.new_view_freq);
		setProperty(props, "fullscreen", fullscreen, defaults.fullscreen);
		setProperty(props, "invert_camera_pitch", invert_camera_pitch, defaults.invert_camera_pitch);
		setProperty(props, "aggressive_units", aggressive_units, defaults.aggressive_units);
		setProperty(props, "use_native_cursor", use_native_cursor, defaults.use_native_cursor);
		setProperty(props, "mapmode_delay", mapmode_delay, defaults.mapmode_delay);
		setProperty(props, "tooltip_delay", tooltip_delay, defaults.tooltip_delay);
		setProperty(props, "first_run", first_run, defaults.first_run);
		setProperty(props, "warning_no_sound", warning_no_sound, defaults.warning_no_sound);

		Path settings_file = LocalInput.getGameDir().resolve(Globals.SETTINGS_FILE_NAME);
		try (OutputStream out = Files.newOutputStream(settings_file)) {
			props.store(out, "comment");
		} catch (IOException e) {
			logger.warning("Failed to write settings to " + settings_file + " exception: " + e);
		}
	}
	
	public void load(@NonNull Path game_dir) {
		Properties props = new Properties();
		Path settings_file = game_dir.resolve(Globals.SETTINGS_FILE_NAME);
		if (!Files.exists(settings_file)) {
			return;
		}
		try (InputStream in = Files.newInputStream(settings_file)) {
			props.load(in);
		} catch (IOException e) {
			logger.warning("WARNING: Could not read settings from " + settings_file + ". Using defaults.");
			return;
		}

		last_event_log_dir = getPath(props, "last_event_log_dir", last_event_log_dir);
		last_revision = getInt(props, "last_revision", last_revision);
		crashed = getBoolean(props, "crashed", crashed);
		username = props.getProperty("username", username);
		pw_digest = props.getProperty("pw_digest", pw_digest);
		remember_login = getBoolean(props, "remember_login", remember_login);
		graphic_detail = getInt(props, "graphic_detail", graphic_detail);
		play_music = getBoolean(props, "play_music", play_music);
		play_sfx = getBoolean(props, "play_sfx", play_sfx);
		music_gain = getFloat(props, "music_gain", music_gain);
		sound_gain = getFloat(props, "sound_gain", sound_gain);
		language = props.getProperty("language", language);
		view_width = getInt(props, "view_width", view_width);
		view_height = getInt(props, "view_height", view_height);
		view_freq = getInt(props, "view_freq", view_freq);
		new_view_width = getInt(props, "new_view_width", new_view_width);
		new_view_height = getInt(props, "new_view_height", new_view_height);
		new_view_freq = getInt(props, "new_view_freq", new_view_freq);
		fullscreen = getBoolean(props, "fullscreen", fullscreen);
		invert_camera_pitch = getBoolean(props, "invert_camera_pitch", invert_camera_pitch);
		aggressive_units = getBoolean(props, "aggressive_units", aggressive_units);
		use_native_cursor = getBoolean(props, "use_native_cursor", use_native_cursor);
		mapmode_delay = getFloat(props, "mapmode_delay", mapmode_delay);
		tooltip_delay = getFloat(props, "tooltip_delay", tooltip_delay);
		first_run = getBoolean(props, "first_run", first_run);
		warning_no_sound = getBoolean(props, "warning_no_sound", warning_no_sound);
	}

	// --- Save Helpers ---
	private void setProperty(@NonNull Properties props, String key, @NonNull Path value, Path defaultValue) {
		if (!value.equals(defaultValue)) {
			props.setProperty(key, value.toString());
		}
	}

	private void setProperty(@NonNull Properties props, String key, @NonNull String value, String defaultValue) {
		if (!value.equals(defaultValue)) {
			props.setProperty(key, value);
		}
	}

	private void setProperty(@NonNull Properties props, String key, int value, int defaultValue) {
		if (value != defaultValue) {
			props.setProperty(key, String.valueOf(value));
		}
	}

	private void setProperty(@NonNull Properties props, String key, float value, float defaultValue) {
		if (value != defaultValue) {
			props.setProperty(key, String.valueOf(value));
		}
	}

	private void setProperty(@NonNull Properties props, String key, boolean value, boolean defaultValue) {
		if (value != defaultValue) {
			props.setProperty(key, String.valueOf(value));
		}
	}

	// --- Load Helpers ---
	private boolean getBoolean(@NonNull Properties props, String key, boolean defaultValue) {
		String value = props.getProperty(key);
		if (value == null) {
			return defaultValue;
		}
		// Boolean.parseBoolean is robust and doesn't throw exceptions
		return Boolean.parseBoolean(value);
	}

	private int getInt(@NonNull Properties props, String key, int defaultValue) {
		String value = props.getProperty(key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			logger.warning("WARNING: Invalid value for setting '" + key + "': '" + value + "'. Using default value '" + defaultValue + "'.");
			return defaultValue;
		}
	}

	private float getFloat(@NonNull Properties props, String key, float defaultValue) {
		String value = props.getProperty(key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			logger.warning("WARNING: Invalid value for setting '" + key + "': '" + value + "'. Using default value '" + defaultValue + "'.");
			return defaultValue;
		}
	}

	private Path getPath(@NonNull Properties props, String key, Path defaultValue) {
		String value = props.getProperty(key);
		if (value == null || value.isEmpty()) {
			return defaultValue;
		}
		try {
			return Path.of(value);
		} catch (InvalidPathException e) {
			logger.warning("Invalid path for setting '" + key + "': '" + value + "'. Using default value '" + defaultValue + "'.");
			return defaultValue;
		}
	}

	private void writeObject(java.io.@NonNull ObjectOutputStream out) throws IOException {
		out.defaultWriteObject();
		out.writeObject(last_event_log_dir.toString());
	}

	private void readObject(java.io.@NonNull ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		last_event_log_dir = Path.of((String) in.readObject());
	}
}
