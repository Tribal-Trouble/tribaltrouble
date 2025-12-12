package com.oddlabs.tt.delegate;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.net.NetworkSelector;
import com.oddlabs.tt.camera.Camera;
import com.oddlabs.tt.form.ConnectingForm;
import com.oddlabs.tt.form.OptionsMenu;
import com.oddlabs.tt.form.QuitForm;
import com.oddlabs.tt.form.SelectGameMenu;
import com.oddlabs.tt.gui.Form;
import com.oddlabs.tt.gui.GUI;
import com.oddlabs.tt.gui.GUIImage;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.KeyboardEvent;
import com.oddlabs.tt.gui.LocalInput;
import com.oddlabs.tt.gui.MenuButton;
import com.oddlabs.tt.landscape.WorldParameters;
import com.oddlabs.tt.net.Client;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.Server;
import com.oddlabs.tt.net.WorldInitAction;
import com.oddlabs.tt.player.Player;
import com.oddlabs.tt.procedural.Landscape;
import com.oddlabs.tt.render.Renderer;
import com.oddlabs.tt.resource.IslandGenerator;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.trigger.GameOverTrigger;
import com.oddlabs.tt.util.Utils;
import com.oddlabs.tt.viewer.InGameInfo;
import com.oddlabs.tt.viewer.MultiplayerInGameInfo;
import com.oddlabs.tt.viewer.WorldViewer;
import com.oddlabs.util.Color;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.net.InetAddress;
import java.util.ResourceBundle;

public abstract class Menu extends CameraDelegate<Camera> {
    static final int COLOR_NORMAL = Color.WHITE_INT;
    static final int COLOR_ACTIVE = 0xFF_FF_CC_9F;
    private static final int MENU_X = 160;
    private static final int overlay_texture_width = 1024;
    private static final int overlay_texture_height = 1024;
    private static final int overlay_image_width = 800;
    private static final int overlay_image_height = 600;
    private static final String overlay_texture_name = "/textures/gui/mainmenu";

    public static final ResourceBundle bundle = ResourceBundle.getBundle(MainMenu.class.getName());

    private final @NonNull NetworkSelector network;

    private @Nullable Form current_menu;
    private boolean current_menu_centered;

    private @Nullable GUIImage overlay;
    private @Nullable GUIImage logo;

    protected Menu(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, @NonNull Camera camera) {
        super(gui_root, camera);
        this.network = network;
        setCanFocus(true);
        setFocusCycle(true);
    }

    final @NonNull NetworkSelector getNetwork() {
        return network;
    }

    private void init() {
        clearChildren();
        int screen_width = LocalInput.getViewWidth();
        int screen_height = LocalInput.getViewHeight();
        overlay = new GUIImage(screen_width, screen_height, 0f, 0f, (float) overlay_image_width / overlay_texture_width, (float) overlay_image_height / overlay_texture_height, overlay_texture_name);
        overlay.setPos(0, 0);
        addChild(overlay);

        String logo_file = Utils.getBundleString(bundle, "logo_file");
        logo = new GUIImage((int) ((347f / 800f) * screen_width), (int) ((206f / 600f) * screen_height), 0f, 0f, 347f / 512f, 206f / 256f, logo_file);
        logo.setPos(0, screen_height - logo.getHeight());
        addChild(logo);
    }

    final void addDefaultOptionsButton() {
        addOptionsButton(() -> new OptionsMenu(getGUIRoot()));
    }

    final void addOptionsButton(@NonNull FormFactory<?> factory) {
        MenuButton options = new MenuButton(Utils.getBundleString(bundle, "options"), COLOR_NORMAL, COLOR_ACTIVE);
        options.addMouseClickListener((_,_,_,_) -> setMenu(factory.create()));
        addChild(options);
    }

    final void addExitButton() {
        MenuButton exit = new MenuButton(Utils.getBundleString(bundle, "quit"), COLOR_NORMAL, COLOR_ACTIVE);
        exit.addMouseClickListener((_,_,_,_) -> setMenuCentered(new QuitForm(getGUIRoot())));
        addChild(exit);
    }

    protected abstract void addButtons();

    final void reload() {
        init();
        addButtons();

        displayChangedNotify(LocalInput.getViewWidth(), LocalInput.getViewHeight());
    }

    @Override
    protected void keyPressed(@NonNull KeyboardEvent event) {
        switch (event.getKeyCode()) {
            case ESCAPE -> {
            }
            default -> super.keyPressed(event);
        }
    }

    @Override
    public void displayChangedNotify(int width, int height) {
        setDim(width, height);

        int y = height - (int) (190f * height / overlay_image_height);
        int x = 15;

        overlay.setDim(width, height);
        logo.setDim((int) ((347f / 800f) * width), (int) ((206f / 600f) * height));
        logo.setPos(0, height - logo.getHeight());
        GUIObject child = getLastChild();
        while (child != null) {
            if (child instanceof MenuButton) {
                child.setPos(x, y - child.getHeight());
                y -= (int) (child.getHeight() * .875);
            }
            child = child.getPrior();
        }
        if (current_menu != null) {
            if (current_menu_centered) {
                current_menu.centerPos();
            } else {
                positionMenu();
            }
        }
    }

    private void disableButtons(boolean disabled) {
        GUIObject child = getLastChild();
        while (child != null) {
            if (child instanceof MenuButton button) {
                button.setDisabled(disabled);
            }
            child = child.getPrior();
        }
    }

    @Override
    public final void setFocus() {
        if (current_menu != null) {
            current_menu.setFocus();
        } else {
            GUIObject child = getLastChild();
            while (child != null) {
                if (child instanceof MenuButton button) {
                    button.setFocus();
                    break;
                }
                child = child.getPrior();
            }
            super.setFocus();
            focusNext();
        }
    }

    @Override
    protected final void keyRepeat(@NonNull KeyboardEvent event) {
        switch (event.getKeyCode()) {
            case TAB -> switchFocus(event.isShiftDown() ? -1 : 1);
            case UP -> focusPrior();
            case DOWN -> focusNext();
            default -> {
            }
        }
    }

    @Override
    public void mouseScrolled(int amount) {
    }

    public final void setMenuCentered(@NonNull Form menu) {
        setMenu(menu);
        menu.centerPos();
        current_menu_centered = true;
    }

    public final void setMenu(@NonNull Form menu) {
        if (current_menu != null)
            current_menu.remove();
        disableButtons(true);
        menu.addCloseListener(() -> {
            disableButtons(false);
            current_menu = null;
        });
        current_menu = menu;
        addChild(current_menu);
        current_menu.setFocus();
        positionMenu();
        current_menu_centered = false;
    }

    private void positionMenu() {
        current_menu.setPos(MENU_X, (LocalInput.getViewHeight() - current_menu.getHeight()) * 2 / 3);
    }

	protected final void addResumeButton() {
        MenuButton resume = new MenuButton(Utils.getBundleString(bundle, "resume"), COLOR_NORMAL, COLOR_ACTIVE);
        addChild(resume);
        resume.addMouseClickListener( (_, _, _, _) -> pop());
    }

    public static void completeGameSetupHack(@NonNull WorldViewer world_viewer) {
        world_viewer.getGUIRoot().pushDelegate(world_viewer.getDelegate());
        Renderer.setMusicPath(world_viewer.getLocalPlayer().getRace().getMusicPath(), 10f);
    }

    public static final class DefaultWorldInitAction implements WorldInitAction {
        @Override
        public void run(@NonNull WorldViewer viewer) {
            new GameOverTrigger(viewer);
            completeGameSetupHack(viewer);
        }
    }

    public final @NonNull GameNetwork joinGame(@NonNull NetworkSelector network, GUI gui, int host_id, boolean rated, int gamespeed, String map_code, SelectGameMenu owner, float random_start_pos, int max_unit_count) {
        GUIRoot gui_root = getGUIRoot();
        Client client = new Client(null, network, gui, host_id, new WorldParameters(gamespeed, map_code, Player.INITIAL_UNIT_COUNT,
                max_unit_count),
                new MultiplayerInGameInfo(random_start_pos, rated),
                new DefaultWorldInitAction());
        GameNetwork game_network = new GameNetwork(null, client);
        ConnectingForm connecting_form = new ConnectingForm(game_network, getGUIRoot(), owner, true);
        client.setConfigurationListener(connecting_form);
        gui_root.addModalForm(connecting_form);
        return game_network;
    }

    public static @NonNull GameNetwork startNewGame(@NonNull NetworkSelector network, @NonNull GUIRoot gui_root, SelectGameMenu owner, WorldParameters world_params, @NonNull InGameInfo ingame_info, WorldInitAction init_action, Game game, int meters_per_world, Landscape.@NonNull TerrainType terrain, float hills, float vegetation_amount, float supplies_amount, int seed, String[] ai_names) {
        boolean multiplayer = ingame_info.isMultiplayer();
        WorldGenerator generator = new IslandGenerator(meters_per_world, terrain, hills, vegetation_amount, supplies_amount, seed);
        InetAddress address = multiplayer ? null : com.oddlabs.util.Utils.getLoopbackAddress();
        final Server server = new Server(network, game, address, generator, multiplayer, ai_names);
        Client client = new Client(server::close, network, gui_root.getGUI(), -1, world_params, ingame_info, init_action);
        GameNetwork game_network = new GameNetwork(server, client);
        ConnectingForm connecting_form = new ConnectingForm(game_network, gui_root, owner, multiplayer);
        client.setConfigurationListener(connecting_form);
        gui_root.addModalForm(connecting_form);
        return game_network;
    }
}
