package com.oddlabs.tt.delegate;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.form.*;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.ImageBuyButton;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.gui.Renderable;
import com.oddlabs.tt.guievent.CloseListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.input.Keyboard;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.net.Client;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.Server;
import com.oddlabs.tt.net.WorldInitAction;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.trigger.GameOverTrigger;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.InGameInfo;
import com.oddlabs.tt.viewer.MultiplayerInGameInfo;
import com.oddlabs.tt.viewer.WorldViewer;

import java.awt.Desktop;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ResourceBundle;

public abstract strictfp class Menu extends CameraDelegate {

    protected static final float[] COLOR_NORMAL = new float[] {1f, 1f, 1f};
    protected static final float[] COLOR_ACTIVE = new float[] {1f, .8f, .63f};
    private static final int MENU_X = 160;

    /** How wide the overlay image is as a png */
    private static final int overlay_texture_width = 1024;

    /** How high the overlay image is as a png */
    private static final int overlay_texture_height = 1024;

    /** The desired width of the overlay image in game */
    private static final int overlay_image_width = 800;

    /** The desired height of the overlay image in game */
    private static final int overlay_image_height = 600;

    /** The name of the overlay texture without its extension */
    private static final String overlay_texture_name = "/textures/gui/mainmenu";

    /** The name of the discord texture without its extension */
    private static final String discord_texture_name = "/textures/gui/discord";

    /** The name of the github texture without its extension */
    private static final String github_texture_name = "/textures/gui/github";

    public static final ResourceBundle bundle = ResourceBundle.getBundle(MainMenu.class.getName());

    private final NetworkSelector network;

    private Form current_menu = null;
    private boolean current_menu_centered;

    private GUIImage overlay;
    private GUIImage logo;
    private GUIImage discord;
    private GUIImage github;

    protected Menu(NetworkSelector network, GUIRoot gui_root, Camera camera) {
        super(gui_root, camera);
        this.network = network;
        setCanFocus(true);
        setFocusCycle(true);
    }

    protected final NetworkSelector getNetwork() {
        return network;
    }

    private void init() {
        clearChildren();
        int screen_width = LocalInput.getViewWidth();
        int screen_height = LocalInput.getViewHeight();
        overlay =
                new GUIImage(
                        screen_width,
                        screen_height,
                        0f,
                        0f,
                        (float) overlay_image_width / overlay_texture_width,
                        (float) overlay_image_height / overlay_texture_height,
                        overlay_texture_name);
        overlay.setPos(0, 0);
        addChild(overlay);

        String logo_file = Utils.getBundleString(bundle, "logo_file");
        logo =
                new GUIImage(
                        (int) ((347f / 800f) * screen_width),
                        (int) ((206f / 600f) * screen_height),
                        0f,
                        0f,
                        347f / 512f,
                        (float) 206f / 256f,
                        logo_file);

        logo.setPos(0, screen_height - logo.getHeight());
        addChild(logo);

        discord = new GUIImage(80, 80, 0f, 0f, (float) 1, (float) 1, discord_texture_name, true);
        github = new GUIImage(76, 76, 0f, 0f, (float) 1, (float) 1, github_texture_name, true);
        github.setPos(screen_width - github.getWidth() - 20, discord.getHeight() / 2 + 4);
        github.addMouseClickListener(new GithubClickedListener());
        addChild(github);

        discord.setPos(
                screen_width - discord.getWidth() - 20 - github.getWidth() - 20,
                discord.getHeight() / 2);
        discord.addMouseClickListener(new DiscordClickedListener());
        addChild(discord);
    }

    protected final void addDefaultOptionsButton() {
        addOptionsButton(
                new FormFactory() {
                    public final Form create() {
                        return new OptionsMenu(getGUIRoot());
                    }
                });
    }

    protected final void addOptionsButton(FormFactory factory) {
        MenuButton options =
                new MenuButton(
                        Utils.getBundleString(bundle, "options"), COLOR_NORMAL, COLOR_ACTIVE);
        addChild(options);
        options.addMouseClickListener(new OptionsListener(factory));
    }

    protected final void addExitButton() {
        MenuButton exit =
                new MenuButton(Utils.getBundleString(bundle, "quit"), COLOR_NORMAL, COLOR_ACTIVE);
        addChild(exit);
        exit.addMouseClickListener(new ExitListener());
    }

    protected abstract void addButtons();

    public final void reload() {
        init();
        addButtons();

        displayChangedNotify(LocalInput.getViewWidth(), LocalInput.getViewHeight());
    }

    protected void keyPressed(KeyboardEvent event) {
        switch (event.getKeyCode()) {
            case Keyboard.KEY_ESCAPE:
                break;
            default:
                super.keyPressed(event);
                break;
        }
    }

    public void displayChangedNotify(int width, int height) {
        getGUIRoot().displayChanged(width, height);
        setDim(width, height);

        int y = height - (int) (190f * height / overlay_image_height);
        int x = 15;

        overlay.setDim(width, height);
        logo.setDim((int) ((347f / 800f) * width), (int) ((206f / 600f) * height));
        logo.setPos(0, height - logo.getHeight());
        Renderable child = getLastChild();
        while (child != null) {
            if (child instanceof MenuButton) {
                child.setPos(x, y - child.getHeight());
                y -= (int) (child.getHeight() * .875);
            } else if (child instanceof ImageBuyButton) {
                ImageBuyButton buy_button = (ImageBuyButton) child;
                buy_button.setPos(width - buy_button.getWidth() - 20, 20);
            }
            child = (Renderable) child.getPrior();
        }
        if (current_menu != null) {
            if (current_menu_centered) {
                current_menu.centerPos();
            } else {
                positionMenu();
            }
        }
    }

    private final void disableButtons(boolean disabled) {
        Renderable child = getLastChild();
        while (child != null) {
            if (child instanceof MenuButton) {
                MenuButton button = (MenuButton) child;
                button.setDisabled(disabled);
            }
            child = (Renderable) child.getPrior();
        }
    }

    public final void setFocus() {
        if (current_menu != null) {
            current_menu.setFocus();
        } else {
            Renderable child = getLastChild();
            while (child != null) {
                if (child instanceof MenuButton) {
                    MenuButton button = (MenuButton) child;
                    button.setFocus();
                    break;
                }
                child = (Renderable) child.getPrior();
            }
            super.setFocus();
            focusNext();
        }
    }

    protected final void keyRepeat(KeyboardEvent event) {
        switch (event.getKeyCode()) {
            case Keyboard.KEY_TAB:
                switchFocus(event.isShiftDown() ? -1 : 1);
                break;
            case Keyboard.KEY_UP:
                focusPrior();
                break;
            case Keyboard.KEY_DOWN:
                focusNext();
                break;
            default:
                break;
        }
    }

    public void mouseScrolled(int amount) {}

    protected void renderGeometry() {}

    public final void setMenuCentered(Form menu) {
        setMenu(menu);
        menu.centerPos();
        current_menu_centered = true;
    }

    public final void setMenu(Form menu) {
        if (current_menu != null) {
            current_menu.remove();
        }
        disableButtons(true);
        menu.addCloseListener(new EscapedListener());
        current_menu = menu;
        addChild(current_menu);
        current_menu.setFocus();
        positionMenu();
        current_menu_centered = false;
    }

    private final void positionMenu() {
        current_menu.setPos(
                MENU_X, (LocalInput.getViewHeight() - current_menu.getHeight()) * 2 / 3);
    }

    protected final void addResumeButton() {
        MenuButton resume =
                new MenuButton(Utils.getBundleString(bundle, "resume"), COLOR_NORMAL, COLOR_ACTIVE);
        addChild(resume);
        resume.addMouseClickListener(
                new MouseClickListener() {
                    public final void mouseClicked(int button, int x, int y, int clicks) {
                        pop();
                    }
                });
    }

    public static void completeGameSetupHack(WorldViewer world_viewer) {
        world_viewer.getGUIRoot().pushDelegate(world_viewer.getDelegate());
        Renderer.setMusicPath(world_viewer.getLocalPlayer().getRace().getMusicPath(), 10f);
    }

    public static final strictfp class DefaultWorldInitAction implements WorldInitAction {

        public final void run(WorldViewer viewer) {
            new GameOverTrigger(viewer);
            completeGameSetupHack(viewer);
        }
    }

    public final GameNetwork joinGame(
            NetworkSelector network,
            GUI gui,
            int host_id,
            boolean rated,
            int gamespeed,
            String map_code,
            SelectGameMenu owner,
            float random_start_pos,
            int max_unit_count) {
        GUIRoot gui_root = getGUIRoot();
        Client client =
                new Client(
                        null,
                        network,
                        gui,
                        host_id,
                        new WorldParameters(
                                gamespeed, map_code, Player.INITIAL_UNIT_COUNT, max_unit_count),
                        new MultiplayerInGameInfo(random_start_pos, rated),
                        new DefaultWorldInitAction());
        GameNetwork game_network = new GameNetwork(null, client);
        ConnectingForm connecting_form =
                new ConnectingForm(game_network, getGUIRoot(), owner, true);
        client.setConfigurationListener(connecting_form);
        gui_root.addModalForm(connecting_form);
        return game_network;
    }

    public static final GameNetwork startNewGame(
            NetworkSelector network,
            GUIRoot gui_root,
            SelectGameMenu owner,
            WorldParameters world_params,
            InGameInfo ingame_info,
            WorldInitAction init_action,
            Game game,
            int meters_per_world,
            int terrain_type,
            float hills,
            float vegetation_amount,
            float supplies_amount,
            int seed,
            String[] ai_names) {
        boolean multiplayer = ingame_info.isMultiplayer();
        WorldGenerator generator =
                new IslandGenerator(
                        meters_per_world,
                        terrain_type,
                        hills,
                        vegetation_amount,
                        supplies_amount,
                        seed);
        InetAddress address = multiplayer ? null : com.oddlabs.util.Utils.getLoopbackAddress();
        final Server server = new Server(network, game, address, generator, multiplayer, ai_names);
        Client client =
                new Client(
                        new Runnable() {
                            public final void run() {
                                server.close();
                            }
                        },
                        network,
                        gui_root.getGUI(),
                        -1,
                        world_params,
                        ingame_info,
                        init_action);
        GameNetwork game_network = new GameNetwork(server, client);
        ConnectingForm connecting_form =
                new ConnectingForm(game_network, gui_root, owner, multiplayer);
        client.setConfigurationListener(connecting_form);
        gui_root.addModalForm(connecting_form);
        return game_network;
    }

    private final strictfp class OptionsListener implements MouseClickListener {

        private final FormFactory factory;

        OptionsListener(FormFactory factory) {
            this.factory = factory;
        }

        public final void mouseClicked(int button, int x, int y, int clicks) {
            setMenu(factory.create());
        }
    }

    private final strictfp class ExitListener implements MouseClickListener {

        public final void mouseClicked(int button, int x, int y, int clicks) {
            setMenuCentered(new QuitForm(getGUIRoot()));
        }
    }

    private final strictfp class EscapedListener implements CloseListener {

        public final void closed() {
            disableButtons(false);
            current_menu = null;
        }
    }

    private final strictfp class DiscordClickedListener implements MouseClickListener {

        public final void mouseClicked(int button, int x, int y, int clicks) {
            try {
                Desktop.getDesktop().browse(new URI("https://discord.gg/j8PZyGBZt5"));
            } catch (IOException | URISyntaxException e) {
                System.out.println("Failed to open Discord link: " + e.getMessage());
            }
        }
    }

    private final strictfp class TribalTroubleClicked implements MouseClickListener {

        public final void mouseClicked(int button, int x, int y, int clicks) {
            try {
                Desktop.getDesktop().browse(new URI("https://tribaltrouble.org"));
            } catch (IOException | URISyntaxException e) {
                System.out.println("Failed to open Tribal Trouble link: " + e.getMessage());
            }
        }
    }

    private final strictfp class GithubClickedListener implements MouseClickListener {

        public final void mouseClicked(int button, int x, int y, int clicks) {
            try {
                Desktop.getDesktop()
                        .browse(new URI("https://github.com/OmarAMokhtar/tribaltrouble"));
            } catch (IOException | URISyntaxException e) {
                System.out.println("Failed to open Github link: " + e.getMessage());
            }
        }
    }
}
