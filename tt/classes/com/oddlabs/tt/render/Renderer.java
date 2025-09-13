package com.oddlabs.tt.render;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryUtil.*;

import com.oddlabs.event.Deterministic;
import com.oddlabs.http.HttpRequestParameters;
import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.net.TaskThread;
import com.oddlabs.net.TimeManager;
import com.oddlabs.regclient.RegistrationClient;
import com.oddlabs.tt.Main;
import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.animation.TimerAnimation;
import com.oddlabs.tt.animation.Updatable;
import com.oddlabs.tt.audio.AbstractAudioPlayer;
import com.oddlabs.tt.audio.AudioManager;
import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.audio.AudioPlayer;
import com.oddlabs.tt.camera.CameraState;
import com.oddlabs.tt.camera.MenuCamera;
import com.oddlabs.tt.delegate.MainMenu;
import com.oddlabs.tt.delegate.QuitScreen;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.form.LoadCallback;
import com.oddlabs.tt.form.MessageForm;
import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.global.GlobalsInit;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.CounterLabel;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.Languages;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.landscape.LandscapeResources;
import com.oddlabs.tt.landscape.NotificationListener;
import com.oddlabs.tt.landscape.TreeSupply;
import com.oddlabs.tt.landscape.World;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.model.Selectable;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.resource.NativeResource;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.resource.WorldInfo;
import com.oddlabs.tt.util.BackBufferRenderer;
import com.oddlabs.tt.util.GLState;
import com.oddlabs.tt.util.GLStateStack;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.tt.util.LoggerOutputStream;
import com.oddlabs.tt.util.StatCounter;
import com.oddlabs.tt.util.StrictGLU;
import com.oddlabs.tt.util.StrictMatrix4f;
import com.oddlabs.tt.util.Target;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.vbo.VBO;
import com.oddlabs.tt.viewer.AmbientAudio;
import com.oddlabs.tt.viewer.Cheat;
import com.oddlabs.tt.viewer.Selection;
import com.oddlabs.updater.UpdateInfo;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALCCapabilities;
import org.lwjgl.openal.ALCapabilities;
import org.lwjgl.opengl.ARBMultisample;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.Platform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.rmi.server.UID;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

public final strictfp class Renderer {

    private static final FloatBuffer matrix_buf = BufferUtils.createFloatBuffer(16);

    private static GLStateStack display_state_stack = new GLStateStack();

    private static final Renderer renderer_instance = new Renderer();
    private static final StatCounter fps = new StatCounter(10);
    private static int num_triangles_rendered;

    private static RegistrationClient registration_client;

    private static boolean grab_frames = false;

    private final Locale default_locale =
            new Locale(
                    Locale.getDefault().getLanguage(), Locale.getDefault().getCountry(), "default");
    private final StrictMatrix4f proj = new StrictMatrix4f();

    private static AbstractAudioPlayer music;
    private static String music_path;
    private static TimerAnimation music_timer;

    private int fallback_val = 0;

    private static boolean finished = false;

    private static Label trial_label;
    private final CameraState frustum_state = new CameraState();
    private CounterLabel trial_counter = null;
    private Label games_left_label;
    private boolean movie_recording_started = false;
    private AmbientAudio ambient;

    public static final float getFPS() {
        return fps.getAveragePerUpdate();
    }

    public static final boolean isRegistered() {
        return true;
        // return registration_client.isRegistered();
    }

    public static final void makeCurrent() {
        try {
            Display.makeCurrent();
            GLStateStack.setCurrent(display_state_stack);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static final void runGame(
            boolean grab_frames,
            boolean eventload,
            boolean zipped,
            boolean silent,
            UpdateInfo update_info) {
        renderer_instance.run(grab_frames, eventload, zipped, silent, update_info);
    }

    public static final Renderer getRenderer() {
        return renderer_instance;
    }

    private final void runGameLoop(NetworkSelector network, GUI gui) {
        AnimationManager.runGameLoop(network, gui, grab_frames);
    }

    private final void setupMatrices(GUIRoot gui_root) {
        // Guard against transient states where no delegate is active yet
        if (gui_root == null || gui_root.getDelegate() == null) {
            return;
        }
        proj.setIdentity();
        multProjection(proj);
        CameraState camera = gui_root.getDelegate().getCamera().getState();
        camera.setView(proj);

        if (!Globals.frustum_freeze) {
            frustum_state.set(camera);
        }
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        proj.store(matrix_buf);
        matrix_buf.rewind();
        GL11.glLoadMatrixf(matrix_buf);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        camera.getModelView().store(matrix_buf);
        matrix_buf.rewind();
        GL11.glLoadMatrixf(matrix_buf);
    }

    public static void multProjection(StrictMatrix4f matrix) {
        StrictGLU.gluPerspective(
                matrix,
                Globals.FOV,
                LocalInput.getViewAspect(),
                Globals.VIEW_MIN,
                Globals.VIEW_MAX);
    }

    public static final void registerTrianglesRendered(int count) {
        num_triangles_rendered += count;
    }

    public static final int getTrianglesRendered() {
        return num_triangles_rendered;
    }

    private final void display(GUI gui) {
        num_triangles_rendered = 0;
        fps.updateDelta(System.currentTimeMillis());
        NativeResource.deleteFinalized();
        // Only setup matrices when a delegate/camera is present
        GUIRoot root = gui.getGUIRoot();
        if (root != null && root.getDelegate() != null) {
            setupMatrices(root);
        }
        gui.render(ambient, frustum_state);
    }

    public static final void shutdownWithQuitScreen(GUIRoot gui_root) {
        if (!isRegistered()) {
            new QuitScreen(gui_root, gui_root.getDelegate().getCamera());
        } else {
            shutdown();
        }
    }

    public static final void shutdown() {
        finished = true;
    }

    public static boolean isFinished() {
        return finished;
    }

    private static final void deleteLog(File log) {
        for (int i = 0; i < com.oddlabs.util.Utils.LOG_FILES.length; i++) {
            File log_file = new File(log, com.oddlabs.util.Utils.LOG_FILES[i]);
            log_file.delete();
        }
        log.delete();
    }

    private static final void deleteOldLogs(File last_log_dir, File new_log_dir, File logs_dir) {
        File[] logs = logs_dir.listFiles();
        if (logs == null) {
            return;
        }
        for (int i = 0; i < logs.length; i++) {
            File log = logs[i];

            if (!log.isDirectory() || log.equals(last_log_dir) || log.equals(new_log_dir)) {
                continue;
            }
            deleteLog(log);
        }
    }

    private final void run(
            boolean grab_frames,
            boolean eventload,
            boolean zipped,
            boolean silent,
            UpdateInfo update_info) {
        Renderer.grab_frames = grab_frames;
        long start_time = System.currentTimeMillis();
        boolean first_frame = true;
        System.out.println("********** Running tt **********");
        System.out.flush();
        String platform_dir;
        if (Platform.get() == Platform.MACOSX) {
            platform_dir = "Library/Application Support" + File.separator;
        } else if (Platform.get() == Platform.LINUX) {
            platform_dir = ".";
        } else {
            platform_dir = "";
        }
        String game_dir_path =
                System.getProperty("user.home") + File.separator + platform_dir + Globals.GAME_NAME;
        File game_dir = new File(game_dir_path);
        Settings settings = Settings.getSettings();

        readOrSetPreference(
                Globals.AFFILIATE_ID_KEY,
                settings.affiliate_id); // setting affiliate id in preferences

        File event_logs_dir = new File(game_dir, "logs");
        File event_log_dir = new File(event_logs_dir, Long.toString(System.currentTimeMillis()));
        setupLoadEvents(eventload, zipped);
        setupEventLogging(event_log_dir, silent);

        // Deterministic logging
        Deterministic deterministic = LocalEventQueue.getQueue().getDeterministic();
        update_info = (UpdateInfo) deterministic.log(update_info);
        game_dir = (File) deterministic.log(game_dir);
        event_log_dir = (File) deterministic.log(event_log_dir);
        settings = (Settings) deterministic.log(settings);

        Languages languages = setupLanguages(deterministic);
        Settings.setSettings(settings);
        File last_event_log_dir = setupBugReporter(deterministic);

        LocalInput _ = new LocalInput();

        NetworkSelector network =
                new NetworkSelector(
                        LocalEventQueue.getQueue().getDeterministic(),
                        new TimeManager() {
                            public final long getMillis() {
                                return LocalEventQueue.getQueue().getMillis();
                            }
                        });

        LocalInput.settings(update_info, game_dir, event_log_dir, settings);
        try {
            initNative(Settings.getSettings().crashed, network);
        } catch (Exception e) {
            // Let it propagate
            System.out.println("Got exception: " + e);
            throw new RuntimeException(e);
        }

        TaskThread task_thread = network.getTaskThread();
        HttpRequestParameters request_parameters = createRegistrationParameters();

        cleanLogs(deterministic, last_event_log_dir, event_log_dir, event_logs_dir);
        Skin.load();
        GUI gui = new GUI(languages);

        GlobalsInit.init();
        DisplayModel.init();
        LocalInput.init();

        long startup_timei = System.currentTimeMillis() - start_time;
        System.out.println("Init done after " + startup_timei);
        ambient = new AmbientAudio(AudioManager.getManager());

        setupMainMenu(network, gui, true);

        boolean reset_keyboard = false;

        try {
            while (!finished) {
                runGameLoop(network, gui);
                if (Display.isALCreated()) {
                    AL10.alListenerf(AL10.AL_GAIN, 1f);
                }
                if (reset_keyboard) {
                    reset_keyboard = false;
                    LocalInput.getLocalInput().resetKeyboard();
                }
                if (!first_frame && !BackBufferRenderer.isBackBufferDirty()) {
                    Display.update();
                }
                display(gui);
                if (first_frame) {
                    long startup_time = System.currentTimeMillis() - start_time;
                    System.out.println(
                            "First frame rendered after " + startup_time + " milliseconds");
                    first_frame = false;
                }
                if (grab_frames && movie_recording_started) {
                    GLUtils.takeScreenshot("");
                }
            }
            LocalEventQueue.getQueue().getDeterministic().setEnabled(true);
            Settings.getSettings().save();
        } finally {
            cleanup();
        }
    }

    /**
     * Checks if the registration file exists in the game directory, and if not, tries to find it in
     * the working directory. If it still doesn't exist, it will try to find it in the /app
     * directory
     *
     * @param game_dir The game directory where the registration file is expected to be
     */
    private File setupRegistrationFile(File game_dir) {
        File registration_file = new File(game_dir, Globals.REG_FILE_NAME);

        if (!registration_file.exists()) {

            // Check if the file is in working dir
            System.out.println("Registration file not found, trying from working directory.");
            System.out.println("Working directory: " + System.getProperty("user.dir"));
            registration_file = new File(System.getProperty("user.dir"), Globals.REG_FILE_NAME);

            // With windows, the working directory is above, so check it in /app
            if (!registration_file.exists()) {
                System.out.println(
                        "Registration file not found, trying from windows working directory.");
                registration_file =
                        new File(System.getProperty("user.dir") + "/app", Globals.REG_FILE_NAME);
            }
        }
        if (!registration_file.exists()) {
            System.out.println("Registration file not found, multiplayer will not work");
        }
        return registration_file;
    }

    /**
     * Sets the language for the game and loads the corresponding language files
     *
     * @param deterministic The deterministic object
     */
    private Languages setupLanguages(Deterministic deterministic) {

        Languages languages = new Languages(Settings.getSettings().inBetaMode());
        String default_language = (String) deterministic.log(Locale.getDefault().getLanguage());
        String language = Settings.getSettings().language;
        if (language.equals("default")) {
            language = default_language;
        }
        if (!languages.hasLanguage(language)) {
            language = "en";
        }
        Locale.setDefault(new Locale(language));
        return languages;
    }

    /**
     * Sets up the bug reporter if the game has crashed and the user has not hidden the bug reporter
     * via the settings file
     *
     * @param deterministic The deterministic object used to allow playback of events
     * @return The directory where the last event log is stored
     */
    private File setupBugReporter(Deterministic deterministic) {
        File last_event_log_dir = new File(Settings.getSettings().last_event_log_dir);
        boolean crashed = Settings.getSettings().crashed;
        if (crashed && !Settings.getSettings().hide_bugreporter && !deterministic.isPlayback()) {
            System.out.println("Starting bug reporter ...");
            System.out.println("Event log dir: " + last_event_log_dir);
            try {
                // TODO: Fix this eventually? And update bug reporter to work / submit issue?
                // For some reason the bug client window will never pop up and the game won't launch
                // do to this
                // URL url = new URL("https://" + settings.getBugreportAddress() +
                // "/reportbug.php");
                // BugClientWindow.showReporter(url, settings.last_revision, last_event_log_dir);
                System.out.println("Bug reporter completed");
            } catch (Exception e) {
                System.out.println("Failed to start bug reporter: " + e);
            }
        }

        return last_event_log_dir;
    }

    /**
     * Cleans up old log files from the last launch of the game unless developer mode is enabled or
     * a log file is being played back.
     *
     * @param deterministic The deterministic object used to allow playback of events
     * @param last_event_log_dir The directory where the last event log is stored
     * @param event_log_dir The directory where the current event log is stored
     * @param event_logs_dir The directory where all event logs are stored
     */
    private void cleanLogs(
            Deterministic deterministic,
            File last_event_log_dir,
            File event_log_dir,
            File event_logs_dir) {
        if (!Settings.getSettings().inDeveloperMode() && !deterministic.isPlayback()) {
            deleteOldLogs(last_event_log_dir, event_log_dir, event_logs_dir);
        }
    }

    /**
     * Sets up steaming event logging to the event_log_dir.
     *
     * @param event_log_dir The directory where the event log files will be written.
     * @param silent If true, the standard output and error streams will not be redirected to the
     *     log files.
     */
    private void setupEventLogging(File event_log_dir, boolean silent) {
        if (LocalEventQueue.getQueue().getDeterministic() == null
                && Settings.getSettings().save_event_log) {
            event_log_dir.mkdirs();
            System.out.println("Writing log files in " + event_log_dir);
            LocalEventQueue.getQueue()
                    .setEventsLogged(
                            new File(
                                    event_log_dir
                                            + File.separator
                                            + com.oddlabs.util.Utils.EVENT_LOG));

            try {
                OutputStream std_err_file =
                        new FileOutputStream(
                                new File(event_log_dir, com.oddlabs.util.Utils.STD_ERR));
                OutputStream std_out_file =
                        new FileOutputStream(
                                new File(event_log_dir, com.oddlabs.util.Utils.STD_OUT));
                OutputStream new_err;
                OutputStream new_out;
                if (!silent) {
                    new_err = new LoggerOutputStream(new OutputStream[] {System.err, std_err_file});
                    new_out = new LoggerOutputStream(new OutputStream[] {System.out, std_out_file});
                } else {
                    new_err = std_err_file;
                    new_out = std_out_file;
                }
                System.setErr(new PrintStream(new_err));
                System.setOut(new PrintStream(new_out));
            } catch (IOException e) {
                System.err.println(
                        "Failed to setup logging to " + event_log_dir + " exception: " + e);
            }
        }
    }

    /**
     * Setups up deterministic logging for events.
     *
     * @param eventload If true, the events will be loaded from a file.
     * @param zipped If true, the event log file path will be treated as a .gz file
     */
    private void setupLoadEvents(boolean eventload, boolean zipped) {
        if (eventload || grab_frames) {
            String last_event_log_path =
                    Settings.getSettings().last_event_log_dir + File.separator + "event.log";
            if (zipped) {
                last_event_log_path += ".gz";
            }
            System.out.println("last_event_log_path = " + last_event_log_path);
            // Only use when anal debugging
            //			ChecksumLogger.initLogging();
            LocalEventQueue.getQueue().loadEvents(new File(last_event_log_path), zipped);
        }
    }

    /** Creates a http request to the registration server with the current affiliate ID */
    private final HttpRequestParameters createRegistrationParameters() {
        String affiliate_id = "";
        try {
            Preferences pref = Preferences.userNodeForPackage(com.oddlabs.tt.render.Renderer.class);
            affiliate_id = pref.get(Globals.AFFILIATE_ID_KEY, "");
        } catch (Exception e) {
        }
        Map parameters = new HashMap();
        parameters.put("current_affiliate_id", Settings.getSettings().affiliate_id);
        parameters.put("affiliate_id", affiliate_id);
        return new HttpRequestParameters(
                "https://"
                        + Settings.getSettings().getRegistrationAddress()
                        + "/oddlabs/registration",
                parameters);
    }

    public final Locale getDefaultLocale() {
        return default_locale;
    }

    private static final void failedOpenGL(Exception e) {
        e.printStackTrace();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception je) {
            je.printStackTrace();
        }

        ResourceBundle bundle = ResourceBundle.getBundle(Renderer.class.getName());
        String title = Utils.getBundleString(bundle, "error_title");
        String message = Utils.getBundleString(bundle, "opengl_error_message");
        int choice =
                JOptionPane.showConfirmDialog(
                        null,
                        message,
                        title,
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) {
            String new_uid = (new UID()).toString();
            String uid = readOrSetPreference("uid", new_uid);
            String os_name = System.getProperty("os.name");
            String os_arch = System.getProperty("os.arch");
            String os_version = System.getProperty("os.version");
            String java_version = System.getProperty("java.version");
            String java_vendor = System.getProperty("java.vendor");
            long total_mem = Runtime.getRuntime().maxMemory();

            try {
                String url =
                        "http://"
                                + Settings.getSettings().getDomainName()
                                + "/driversupport.php?"
                                + "uid="
                                + URLEncoder.encode(uid, "UTF-8")
                                + "&raw_os="
                                + URLEncoder.encode(os_name, "UTF-8")
                                + "&os_version="
                                + URLEncoder.encode(os_version, "UTF-8")
                                + "&arch="
                                + URLEncoder.encode(os_arch, "UTF-8")
                                + "&java_version="
                                + URLEncoder.encode(java_version, "UTF-8")
                                + "&java_vendor="
                                + URLEncoder.encode(java_vendor, "UTF-8")
                                + "&total_mem="
                                + URLEncoder.encode("" + total_mem, "UTF-8");
                // Sys.openURL(url);
            } catch (java.io.UnsupportedEncodingException uee) {
                uee.printStackTrace();
            }
        }

        Main.shutdown();
    }

    private static boolean readOrSetPreference(String key, boolean value) {
        String result_string = readOrSetPreference(key, "" + value);
        return Boolean.valueOf(result_string).booleanValue();
    }

    private static int readOrSetPreference(String key, int value) {
        String result_string = readOrSetPreference(key, "" + value);
        try {
            int result = Integer.parseInt(result_string);
            return result;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String readOrSetPreference(String key, String value) {
        try {
            Preferences pref = Preferences.userNodeForPackage(com.oddlabs.tt.render.Renderer.class);
            String result = pref.get(key, null);
            if (result == null) {
                pref.put(key, value);
                return value;
            } else {
                return result;
            }
        } catch (Exception e) {
            System.out.println("Could not access preferences");
            return value;
        }
    }

    public static final void startMenu(NetworkSelector network, GUI gui) {
        setupMainMenu(network, gui, false);
    }

    private static void setupMainMenu(
            final NetworkSelector network, GUI gui, final boolean first_progress) {
        final WorldGenerator generator =
                new IslandGenerator(
                        256,
                        Landscape.NATIVE,
                        Globals.LANDSCAPE_HILLS,
                        Globals.LANDSCAPE_VEGETATION,
                        Globals.LANDSCAPE_RESOURCES,
                        Globals.LANDSCAPE_SEED,
                        false);
        ProgressForm.setProgressForm(
                network,
                gui,
                new LoadCallback() {
                    public final UIRenderer load(GUIRoot gui_root) {
                        return finishMainMenu(network, gui_root, first_progress, generator);
                    }
                },
                first_progress);
    }

    private static UIRenderer finishMainMenu(
            NetworkSelector network,
            GUIRoot gui_root,
            boolean first_progress,
            WorldGenerator generator) {
        AnimationManager.freezeTime();
        PlayerInfo player_info = new PlayerInfo(0, 0, "");
        RenderQueues render_queues = new RenderQueues();
        LandscapeResources landscape_resources = World.loadCommon(render_queues);
        WorldParameters world_params =
                new WorldParameters(Game.GAMESPEED_NORMAL, "", 2, Player.DEFAULT_MAX_UNIT_COUNT);
        PlayerInfo[] players = new PlayerInfo[] {player_info};
        WorldInfo world_info =
                generator.generate(players.length, world_params.getInitialUnitCount(), 0f);
        World world =
                World.newWorld(
                        AudioManager.getManager(),
                        landscape_resources,
                        null,
                        LandscapeResources.loadTreeLowDetails(),
                        new NotificationListener() {
                            public final void gamespeedChanged(int speed) {}

                            public final void playerGamespeedChanged() {}

                            public final void newAttackNotification(Selectable target) {}

                            public final void newSelectableNotification(Selectable target) {}

                            public final void registerTarget(Target target) {}

                            public final void unregisterTarget(Target target) {}

                            public final void updateTreeLowDetail(
                                    StrictMatrix4f matrix, TreeSupply tree) {}

                            public final void patchesEdited(
                                    int patch_x0, int patch_y0, int patch_x1, int patch_y1) {}
                        },
                        world_params,
                        world_info,
                        generator.getTerrainType(),
                        players,
                        new float[][] {Player.COLORS[0]});
        AnimationManager manager = new AnimationManager();
        LandscapeRenderer landscape_renderer =
                new LandscapeRenderer(world, world_info, gui_root, manager);
        Player local_player = world.getPlayers()[0];
        Selection selection = new Selection(local_player);
        UIRenderer renderer =
                new DefaultRenderer(
                        new Cheat(),
                        local_player,
                        render_queues,
                        generator.getTerrainType(),
                        world_info,
                        landscape_renderer,
                        new Picker(
                                manager,
                                local_player,
                                render_queues,
                                landscape_renderer,
                                selection),
                        selection,
                        generator);
        setMusicPath("/music/menu.ogg", 0f);
        MainMenu main_menu = new MainMenu(network, gui_root, new MenuCamera(world, manager));
        gui_root.pushDelegate(main_menu);
        /*
        if (first_progress && Settings.getSettings().warning_no_sound && !LocalInput.alIsCreated()) {
        	ResourceBundle bundle = ResourceBundle.getBundle(Renderer.class.getName());
        	gui_root.addModalForm(new WarningForm(Utils.getBundleString(bundle, "sound_not_available_caption"), Utils.getBundleString(bundle, "sound_not_available_message")));
        }*/
        if (!initNetwork(network)) {
            //		if (true) {
            ResourceBundle bundle = ResourceBundle.getBundle(Renderer.class.getName());
            gui_root.addModalForm(
                    new MessageForm(
                            Utils.getBundleString(bundle, "network_not_available_caption"),
                            Utils.getBundleString(bundle, "network_not_available_message"),
                            Utils.getBundleString(bundle, "quit"),
                            new MouseClickListener() {
                                public final void mouseClicked(
                                        int button, int x, int y, int clicks) {
                                    shutdown();
                                }
                            }));
        }
        // We'll leave out the reporting, since checksum errors can happen when a peer is
        // disconnected halway through it's EOT
        // broadcast
        /*		if (Globals.checksum_error_in_last_game) {
        Globals.checksum_error_in_last_game = false;
        ResourceBundle bundle = ResourceBundle.getBundle(Renderer.class.getName());
        GUIRoot.getGUIRoot().addModalForm(new QuestionForm(Utils.getBundleString(bundle, "checksum_error_message"), new BugReportListener()));
        }*/
        return renderer;
    }

    private static boolean initNetwork(NetworkSelector network) {
        boolean is_network_created;
        try {
            network.initSelector();
            com.oddlabs.util.Utils.tryGetLoopbackAddress();
            is_network_created = true;
        } catch (IOException e) {
            System.err.println("Failed to initialize network: " + e);
            is_network_created = false;
        }
        return LocalEventQueue.getQueue().getDeterministic().log(is_network_created);
    }

    public final void startMovieRecording() {
        System.out.println("ACTION!");
        movie_recording_started = true;
    }

    public final void cleanup() {
        System.out.println("Cleaning up...");
        LocalEventQueue.getQueue().dispose();
        destroyNative();
        System.out.println("Cleanup complete. Exiting");
    }

    public static final void resetInput() {
        LocalInput.resetKeys();
    }

    private static final void destroyNative() {
        // destroyAL();
        Display.destroy();
    }

    public static final void dumpWindowInfo() {
        int r = GLUtils.getGLInteger(GL11.GL_RED_BITS);
        int g = GLUtils.getGLInteger(GL11.GL_GREEN_BITS);
        int b = GLUtils.getGLInteger(GL11.GL_BLUE_BITS);
        int a = GLUtils.getGLInteger(GL11.GL_ALPHA_BITS);
        int depth = GLUtils.getGLInteger(GL11.GL_DEPTH_BITS);
        int stencil = GLUtils.getGLInteger(GL11.GL_STENCIL_BITS);
        int sample_buffers = 0;
        int samples = 0;
        if (GL.getCapabilities().GL_ARB_multisample) {
            sample_buffers = GLUtils.getGLInteger(ARBMultisample.GL_SAMPLE_BUFFERS_ARB);
            samples = GLUtils.getGLInteger(ARBMultisample.GL_SAMPLES_ARB);
        }
        System.out.println(
                "r = "
                        + r
                        + " | g = "
                        + g
                        + " | b = "
                        + b
                        + " | a = "
                        + a
                        + " | depth = "
                        + depth
                        + " | stencil = "
                        + stencil
                        + " | sample_buffers = "
                        + sample_buffers
                        + " | samples = "
                        + samples);
    }

    private final void initNative(boolean crashed, NetworkSelector network) throws Exception {
        String os_name = System.getProperty("os.name");
        System.out.println("os_name = '" + os_name + "'");
        String os_arch = System.getProperty("os.arch");
        System.out.println("os_arch = '" + os_arch + "'");
        String os_version = System.getProperty("os.version");
        System.out.println("os_version = '" + os_version + "'");
        String java_version = System.getProperty("java.version");
        System.out.println("java_version = '" + java_version + "'");
        String java_vendor = System.getProperty("java.vendor");
        System.out.println("java_vendor = '" + java_vendor + "'");
        long total_mem = Runtime.getRuntime().maxMemory();
        System.out.println("total_mem = '" + total_mem + "'");

        try {
            // AL.create(null, -1, -1, false);
            initAL();
        } catch (Exception e) {
            System.err.println("Could not create sound system: " + e);
        }

        makeCurrent();
        String version = GL11.glGetString(GL11.GL_VERSION);
        System.out.println("GL version: '" + version + "'");
        String vendor = GL11.glGetString(GL11.GL_VENDOR);
        System.out.println("GL vendor: '" + vendor + "'");
        String renderer = GL11.glGetString(GL11.GL_RENDERER);
        System.out.println("GL renderer: '" + renderer + "'");
        String extensions = GL11.glGetString(GL11.GL_EXTENSIONS);
        System.out.println("GL extensions: '" + extensions + "'");

        if (GLUtils.isIntelGMA950()
                || !Settings.getSettings().useTextureCompression()) { // Intel mac mini hack
            Globals.disableTextureCompression();
        }

        dumpWindowInfo();
        try {
            if (!GL.getCapabilities().OpenGL13) {
                if (!GL.getCapabilities().GL_ARB_multitexture) {
                    throw new Exception(
                            "Neither OpenGL 1.3 nor GL_ARB_multitexture is supported, one of which"
                                    + " is required for the game to run. Please upgrade your video"
                                    + " drivers and/or video card.");
                }
                System.out.println(
                        "OpenGL 1.3 is not supported, using GL_ARB_multitexture and"
                                + " GL_ARB_texture_compression instead");
            } else {
                System.out.println("OpenGL 1.3 is supported");
            }
            //			int num_tex_units = GLUtils.getGLInteger(GL13.GL_MAX_TEXTURE_UNITS);
            //			if (num_tex_units < 2)
            //				throw new Exception("Number of texture units " + num_tex_units + " < 2");
        } catch (Exception e) {
            // destroyAL();
            e.printStackTrace();
            System.out.println("Got exception: " + e);
            Display.destroy();
            failedOpenGL(e);
            throw e;
        }
        if (!GL.getCapabilities().OpenGL12) {
            System.out.println("OpenGL 1.2 is not supported");
        }
        display_state_stack = new GLStateStack();
        GLStateStack.setCurrent(display_state_stack);
        resetInput();
        System.out.println("vsync = " + Settings.getSettings().vsync);
        System.out.println("vbo = " + Settings.getSettings().useVBO());
        System.out.println("pbuffer = " + Settings.getSettings().usePbuffer());
        System.out.println("fbo = " + Settings.getSettings().useFBO());
        System.out.println(
                "use_texture_compression = " + Settings.getSettings().useTextureCompression());
        /*if (Settings.getSettings().vsync)
        Display.setVSyncEnabled(true);*/
        initGL();
        initVisibleGL();
    }

    private final void initAL() throws IllegalStateException {
        long device = alcOpenDevice((ByteBuffer) null);
        if (device == NULL) {
            throw new IllegalStateException("Failed to open default audio device");
        }

        long context = alcCreateContext(device, (IntBuffer) null);
        if (context == NULL) {
            throw new IllegalStateException("Failed to create OpenAL context");
        }

        alcMakeContextCurrent(context);
        ALCCapabilities alcCaps = createCapabilities(device);
        ALCapabilities alCaps = AL.createCapabilities(alcCaps);

        AL10.alDistanceModel(AL10.AL_INVERSE_DISTANCE);
    }

    public final void toggleSound() {
        Settings.getSettings().play_sfx = !Settings.getSettings().play_sfx;
        if (Settings.getSettings().play_sfx) {
            AudioManager.getManager().startSources();
        } else {
            AudioManager.getManager().stopSources();
        }
    }

    public final void toggleMusic() {
        Settings.getSettings().play_music = !Settings.getSettings().play_music;
        if (Settings.getSettings().play_music) {
            initMusicPlayer();
        } else if (music != null) {
            music.stop(2.5f, Settings.getSettings().music_gain);
        }
    }

    private static final void initMusicPlayer() {
        music =
                AudioManager.getManager()
                        .newAudio(
                                new AudioParameters(
                                        music_path,
                                        0f,
                                        0f,
                                        0f,
                                        AudioPlayer.AUDIO_RANK_MUSIC,
                                        AudioPlayer.AUDIO_DISTANCE_MUSIC,
                                        Settings.getSettings().music_gain,
                                        1f,
                                        1f,
                                        true,
                                        true,
                                        true));
    }

    public static final void setMusicPath(String music_path, float delay) {
        if (Display.isALCreated()) {
            if (music != null && Settings.getSettings().play_music) {
                music.stop(2.5f, Settings.getSettings().music_gain);
            }
            Renderer.music_path = music_path;
            if (Settings.getSettings().play_music) {
                if (music_timer != null) {
                    music_timer.stop();
                }
                music_timer = new TimerAnimation(new MusicTimer(), delay);
                music_timer.start();
            }
        }
    }

    private static final class MusicTimer implements Updatable {

        public final void update(Object anim) {
            if (music_timer != null) {
                music_timer.stop();
            }
            music_timer = null;
            if (Settings.getSettings().play_music) {
                initMusicPlayer();
            }
        }
    }

    public static final AbstractAudioPlayer getMusicPlayer() {
        return music;
    }

    private static final void destroyAL() {
        if (Display.isALCreated()) {
            AudioManager.getManager().destroy();
            // AL.destroy();
        }
    }

    private final void initVisibleGL() {
        if (Settings.getSettings().fullscreen_depth_workaround) {
            IntBuffer dummy_buf = BufferUtils.createIntBuffer(1);
            GL11.glReadPixels(0, 0, 1, 1, GL11.GL_DEPTH_COMPONENT, GL11.GL_UNSIGNED_INT, dummy_buf);
        }

        FloatBuffer float_array = BufferUtils.createFloatBuffer(4);
        GL11.glEnable(GL11.GL_LIGHT0);
        float[] light_diff_color = {1.0f, 1.0f, 1.0f, 1.0f};
        float_array.put(light_diff_color);
        float_array.rewind();
        GL11.glLightfv(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, float_array);
        GL11.glLightModeli(GL11.GL_LIGHT_MODEL_LOCAL_VIEWER, 1);

        float[] global_ambient = {0.65f, 0.65f, 0.65f, 1.0f};
        float_array.put(global_ambient);
        float_array.rewind();
        GL11.glLightModelfv(GL11.GL_LIGHT_MODEL_AMBIENT, float_array);
        float[] material_color = {1.0f, 1.0f, 1.0f, 1.0f};
        float_array.put(material_color);
        float_array.rewind();
        GL11.glMaterialfv(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE, float_array);
        //	Display.update();
    }

    public static final void initGL() {
        VBO.releaseAll();
        GL11.glFrontFace(GL11.GL_CCW);
        GL11.glCullFace(GL11.GL_BACK);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES, 0);

        GL11.glPixelStorei(GL11.GL_UNPACK_ROW_LENGTH, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_PIXELS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_SKIP_ROWS, 0);
        GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);
        GL11.glPixelStorei(GL11.GL_UNPACK_SWAP_BYTES, 0);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        //		GL11.glAlphaFunc(GL11.GL_GREATER, Globals.ALPHA_CUTOFF);
        // Setup landscape texture coordinate gen
        GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
        GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_REPLACE);
        GLState.activeTexture(GL13.GL_TEXTURE1);
        GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_DECAL);
        GL11.glTexGeni(GL11.GL_S, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
        GL11.glTexGeni(GL11.GL_T, GL11.GL_TEXTURE_GEN_MODE, GL11.GL_OBJECT_LINEAR);
        GLState.activeTexture(GL13.GL_TEXTURE0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glPointSize(7.0f);
        clearScreen();
        GL11.glClearDepth(1.0);
    }

    public static final void clearScreen() {
        GL11.glClearColor(0f, 0f, 0f, 0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        // GL11.glClearColor(1f, 0f, 1f, 0f);
    }
}
