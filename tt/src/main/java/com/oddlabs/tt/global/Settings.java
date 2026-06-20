package com.oddlabs.tt.global;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.MatchmakingServerInterface;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Global game settings and configuration persistence.
 */
public final class Settings implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // event logging
    private static final Logger logger = Logger.getLogger(Settings.class.getName());

    public static final Color.Standard[] DEFAULT_TEAM_COLOURS = {
            new Color.Standard(0xFFFFBF00), /* Orange */
            new Color.Standard(0xFF007FFF), /* Royal Blue */
            new Color.Standard(0xFFFF0040), /* Red */
            new Color.Standard(0xFF00FFBF), /* Teal */
            new Color.Standard(0xFFBF00FF), /* Purple */
            new Color.Standard(0xFFBFFF00) /* Lime */
    };

    public transient @NonNull Path last_event_log_dir = Path.of("");
    public int last_revision = -1;
    public boolean crashed = false;

    // network
    public @NonNull String username = "";
    public @NonNull String pw_digest = "";
    public boolean remember_login = false;

    public int graphic_detail = Globals.DETAIL_NORMAL;

    // sound
    public boolean play_music = true;
    public boolean play_sfx = true;
    public boolean headphone_mode = false;
    public boolean classic_audio = true;
    public float music_gain = .5f;
    public float sound_gain = 1f;

    // language
    public @NonNull String language = "default";

    // window
    public int view_width = -1;
    public int view_height = -1;
    public int view_freq = -1;
    public int view_samples = 4;

    public int new_view_width = view_width;
    public int new_view_height = view_height;
    public int new_view_freq = view_freq;
    public int new_view_samples = 4;

    public boolean fullscreen = true;
    public final boolean vsync = true;
//	public int view_bpp = 32;

    // control
    public boolean invert_camera_pitch = false;
    public boolean aggressive_units = false;
    public boolean show_compass = true;
    public boolean confine_cursor = true;

    // gameplay
    public int gamespeed = Game.GAMESPEED_NORMAL;

    public float mapmode_delay = .5f;
    public float tooltip_delay = .5f;
    public float ui_scale = 0.0f; // 0.0 = 100% (Scale 1.0), 1.0 = Max Scale
    private final boolean developer_mode = Boolean.getBoolean("com.oddlabs.tt.developer");
    public boolean has_native_campaign = developer_mode;

    public final boolean save_event_log = true;
    public boolean generate_dummy_worlds = false;
    public boolean first_run = true;

    public boolean warning_no_sound = true;

    public boolean hide_multiplayer = false;

    // Server domain
    public static final String OFFICIAL_DOMAIN = "tribaltrouble.org";
    private String domain_name = OFFICIAL_DOMAIN;

    // Community links
    public static final String DISCORD_URL = "https://discord.gg/TribalTrouble";
    public static final String GITHUB_URL = "https://github.com/OmarAMokhtar/tribaltrouble";

    public final int frame_grab_milliseconds_per_frame = 40;

    // Accessibility
    public int cvd_mode = 0; // 0=None, 1=Protanopia, 2=Deuteranopia, 3=Tritanopia
    public float cvd_intensity = 1.0f;
    public boolean high_contrast = false;
    public float contrast_intensity = 0.5f;
    public boolean invert_colours = false;
    public float contrast_brightness = 0.0f;
    public float contrast_clarity = 0.0f;
    public boolean team_stencil = false;
    public boolean sound_emojis = true;

    public Color.@NonNull Standard @NonNull [] team_colours = generateTeamColours();

    /**
     * Builds the team-colour palette sized to {@link MatchmakingServerInterface#MAX_PLAYERS}. Uses the hand-picked
     * {@link #DEFAULT_TEAM_COLOURS} for the first slots and fills any remaining slots with evenly-spaced
     * HSB-generated colours.
     */
    private static Color.@NonNull Standard @NonNull [] generateTeamColours() {
        Color.Standard[] all = new Color.Standard[MatchmakingServerInterface.MAX_PLAYERS];
        int copyCount = Math.min(DEFAULT_TEAM_COLOURS.length, all.length);
        System.arraycopy(DEFAULT_TEAM_COLOURS, 0, all, 0, copyCount);
        for (int i = copyCount; i < all.length; i++) {
            float hue = (i - DEFAULT_TEAM_COLOURS.length) / (float) (all.length - DEFAULT_TEAM_COLOURS.length);
            int rgb = java.awt.Color.HSBtoRGB(hue, 0.8f, 0.9f);
            all[i] = new Color.Standard(0xFF000000 | (rgb & 0x00FFFFFF));
        }
        return all;
    }

    public transient Color.Linear @NonNull [] linear_team_colours = Arrays.stream(team_colours)
            .map(Color.Linear::new)
            .toArray(Color.Linear[]::new);

    public Settings() {
    }

    public void updateLinearColors() {
        linear_team_colours = Arrays.stream(team_colours)
                .map(Color.Linear::new)
                .toArray(Color.Linear[]::new);
    }

    public boolean inDeveloperMode() {
        return developer_mode;
    }

    public void setDomain(String new_domain) {
        domain_name = new_domain;
    }

    public String getDomainName() {
        return domain_name;
    }

    public boolean isOfficialServer() {
        if (OFFICIAL_DOMAIN.equals(domain_name) || domain_name.endsWith("." + OFFICIAL_DOMAIN)) {
            return true;
        }
        // Also allow Steam auth on localhost if debug flag is enabled
        if (Globals.debug_steam_auth_localhost
                && ("localhost".equals(domain_name) || "local".equals(domain_name))) {
            return true;
        }
        return false;
    }

    public String getMatchmakingAddress() {
        return "matchmaking." + domain_name;
    }

    public String getRouterAddress() {
        return "router." + domain_name;
    }

    public void save() {
        if (Renderer.getRenderer().getEventQueue().getDeterministic().isPlayback())
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
        setProperty(props, "headphone_mode", headphone_mode, defaults.headphone_mode);
        setProperty(props, "classic_audio", classic_audio, defaults.classic_audio);
        setProperty(props, "music_gain", music_gain, defaults.music_gain);
        setProperty(props, "sound_gain", sound_gain, defaults.sound_gain);
        setProperty(props, "language", language, defaults.language);
        setProperty(props, "view_width", view_width, defaults.view_width);
        setProperty(props, "view_height", view_height, defaults.view_height);
        setProperty(props, "view_freq", view_freq, defaults.view_freq);
        setProperty(props, "new_view_width", new_view_width, defaults.new_view_width);
        setProperty(props, "new_view_height", new_view_height, defaults.new_view_height);
        setProperty(props, "new_view_freq", new_view_freq, defaults.new_view_freq);
        setProperty(props, "new_view_samples", new_view_samples, defaults.new_view_samples);
        setProperty(props, "fullscreen", fullscreen, defaults.fullscreen);
        setProperty(props, "view_samples", view_samples, defaults.view_samples);
        setProperty(props, "invert_camera_pitch", invert_camera_pitch, defaults.invert_camera_pitch);
        setProperty(props, "aggressive_units", aggressive_units, defaults.aggressive_units);
        setProperty(props, "show_compass", show_compass, defaults.show_compass);
        setProperty(props, "confine_cursor", confine_cursor, defaults.confine_cursor);
        setProperty(props, "gamespeed", gamespeed, defaults.gamespeed);
        setProperty(props, "mapmode_delay", mapmode_delay, defaults.mapmode_delay);
        setProperty(props, "tooltip_delay", tooltip_delay, defaults.tooltip_delay);
        setProperty(props, "ui_scale", ui_scale, defaults.ui_scale);
        setProperty(props, "first_run", first_run, defaults.first_run);
        setProperty(props, "warning_no_sound", warning_no_sound, defaults.warning_no_sound);
        setProperty(props, "domain_name", domain_name, defaults.domain_name);

        setProperty(props, "cvd_mode", cvd_mode, defaults.cvd_mode);
        setProperty(props, "cvd_intensity", cvd_intensity, defaults.cvd_intensity);
        setProperty(props, "high_contrast", high_contrast, defaults.high_contrast);
        setProperty(props, "contrast_intensity", contrast_intensity, defaults.contrast_intensity);
        setProperty(props, "invert_colours", invert_colours, defaults.invert_colours);
        setProperty(props, "contrast_brightness", contrast_brightness, defaults.contrast_brightness);
        setProperty(props, "contrast_clarity", contrast_clarity, defaults.contrast_clarity);
        setProperty(props, "team_stencil", team_stencil, defaults.team_stencil);
        setProperty(props, "sound_emojis", sound_emojis, defaults.sound_emojis);
        setProperty(props, "team_colours", team_colours, defaults.team_colours);

        Renderer.getLocalInput().getInputManager().saveBindings(props);

        Path settings_file = Renderer.getLocalInput().getGameDir().resolve(Globals.getSettingsFileName());
        try (OutputStream out = Files.newOutputStream(settings_file)) {
            props.store(out, Instant.now().toString());
        } catch (IOException e) {
            logger.warning("Failed to write settings to " + settings_file + " exception: " + e);
        }
    }

    public void load(@NonNull Path game_dir) {
        Properties props = new Properties();
        Path settings_file = game_dir.resolve(Globals.getSettingsFileName());
        if (!Files.exists(settings_file)) {
            return;
        }
        try (InputStream in = Files.newInputStream(settings_file)) {
            props.load(in);
        } catch (IOException _) {
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
        headphone_mode = getBoolean(props, "headphone_mode", headphone_mode);
        classic_audio = getBoolean(props, "classic_audio", classic_audio);
        music_gain = getFloat(props, "music_gain", music_gain);
        sound_gain = getFloat(props, "sound_gain", sound_gain);
        language = props.getProperty("language", language);
        view_width = getInt(props, "view_width", view_width);
        view_height = getInt(props, "view_height", view_height);
        view_freq = getInt(props, "view_freq", view_freq);
        new_view_width = getInt(props, "new_view_width", new_view_width);
        new_view_height = getInt(props, "new_view_height", new_view_height);
        new_view_freq = getInt(props, "new_view_freq", new_view_freq);
        new_view_samples = getInt(props, "new_view_samples", new_view_samples);
        fullscreen = getBoolean(props, "fullscreen", fullscreen);
        view_samples = getInt(props, "view_samples", view_samples);
        invert_camera_pitch = getBoolean(props, "invert_camera_pitch", invert_camera_pitch);
        aggressive_units = getBoolean(props, "aggressive_units", aggressive_units);
        show_compass = getBoolean(props, "show_compass", show_compass);
        confine_cursor = getBoolean(props, "confine_cursor", confine_cursor);
        gamespeed = getInt(props, "gamespeed", gamespeed);
        mapmode_delay = getFloat(props, "mapmode_delay", mapmode_delay);
        tooltip_delay = getFloat(props, "tooltip_delay", tooltip_delay);
        ui_scale = getFloat(props, "ui_scale", ui_scale);
        first_run = getBoolean(props, "first_run", first_run);
        warning_no_sound = getBoolean(props, "warning_no_sound", warning_no_sound);
        domain_name = props.getProperty("domain_name", domain_name);

        cvd_mode = getInt(props, "cvd_mode", cvd_mode);
        cvd_intensity = getFloat(props, "cvd_intensity", cvd_intensity);
        high_contrast = getBoolean(props, "high_contrast", high_contrast);
        contrast_intensity = getFloat(props, "contrast_intensity", contrast_intensity);
        invert_colours = getBoolean(props, "invert_colours", invert_colours);
        contrast_brightness = getFloat(props, "contrast_brightness", contrast_brightness);
        contrast_clarity = getFloat(props, "contrast_clarity", contrast_clarity);
        team_stencil = getBoolean(props, "team_stencil", team_stencil);
        sound_emojis = getBoolean(props, "sound_emojis", sound_emojis);
        team_colours = getColours(props, "team_colours", team_colours);
        updateLinearColors();

        Renderer.getLocalInput().getInputManager().loadBindings(props);
    }

    // --- Save Helpers ---
    private void setProperty(@NonNull Properties props, @NonNull String key, @NonNull Path value, Path defaultValue) {
        if (!value.equals(defaultValue)) {
            props.setProperty(key, value.toString());
        }
    }

    private void setProperty(@NonNull Properties props, @NonNull String key, @NonNull String value,
            String defaultValue) {
        if (!value.equals(defaultValue)) {
            props.setProperty(key, value);
        }
    }

    private void setProperty(@NonNull Properties props, @NonNull String key, int value, int defaultValue) {
        if (value != defaultValue) {
            props.setProperty(key, String.valueOf(value));
        }
    }

    private void setProperty(@NonNull Properties props, @NonNull String key, float value, float defaultValue) {
        if (value != defaultValue) {
            props.setProperty(key, String.valueOf(value));
        }
    }

    private void setProperty(@NonNull Properties props, @NonNull String key, boolean value, boolean defaultValue) {
        if (value != defaultValue) {
            props.setProperty(key, String.valueOf(value));
        }
    }

    private void setProperty(@NonNull Properties props, @NonNull String key, Color.@NonNull Standard @NonNull [] value,
            Color.@NonNull Standard @NonNull [] defaultValue) {
        if (!Arrays.equals(value, defaultValue)) {
            String colors = Arrays.stream(value)
                    .mapToInt(Color.Standard::toInt)
                    .mapToObj(Integer::toHexString)
                    .collect(Collectors.joining(","));

            props.setProperty(key, colors);
        }
    }

    // --- Load Helpers ---
    private boolean getBoolean(@NonNull Properties props, @NonNull String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        // Boolean.parseBoolean is robust and doesn't throw exceptions
        return Boolean.parseBoolean(value);
    }

    private int getInt(@NonNull Properties props, @NonNull String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException _) {
            logger.warning("WARNING: Invalid value for setting '" + key + "': '" + value + "'. Using default value '"
                    + defaultValue + "'.");
            return defaultValue;
        }
    }

    private float getFloat(@NonNull Properties props, @NonNull String key, float defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException _) {
            logger.warning("WARNING: Invalid value for setting '" + key + "': '" + value + "'. Using default value '"
                    + defaultValue + "'.");
            return defaultValue;
        }
    }

    private static Path getPath(@NonNull Properties props, @NonNull String key, Path defaultValue) {
        String value = props.getProperty(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Path.of(value);
        } catch (InvalidPathException _) {
            logger.warning("Invalid path for setting '" + key + "': '" + value + "'. Using default value '"
                    + defaultValue + "'.");
            return defaultValue;
        }
    }

    private static Color.@NonNull Standard @NonNull [] getColours(@NonNull Properties props, @NonNull String key,
            Color.@NonNull Standard @NonNull [] defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            String[] hexStrings = value.split(",");
            Color.Standard[] result = new Color.Standard[defaultValue.length];
            Arrays.setAll(result, i -> {
                if (i < hexStrings.length) {
                    try {
                        int argb = (int) Long.parseLong(hexStrings[i], 16);
                        return new Color.Standard(argb);
                    } catch (NumberFormatException _) {
                        // ignore invalid color constants
                    }
                }
                return defaultValue[i];
            });

            return result;
        } catch (Exception e) {
            logger.warning("WARNING: Invalid value for setting '" + key + "': '" + value
                    + "'. Using default value. Error: " + e);
            return defaultValue;
        }
    }

    @Serial
    private void writeObject(@NonNull ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(last_event_log_dir.toString());
    }

    @Serial
    private void readObject(@NonNull ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        last_event_log_dir = Path.of((String) in.readObject());
    }
}
