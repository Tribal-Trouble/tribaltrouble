package com.oddlabs.tt.form;

import com.oddlabs.matchmaking.Game;
import com.oddlabs.matchmaking.GameSession;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.font.Font;
import com.oddlabs.tt.global.Settings;
import com.oddlabs.tt.gui.Box;
import com.oddlabs.tt.gui.Diode;
import com.oddlabs.tt.gui.EditLine;
import com.oddlabs.tt.gui.FocusDirection;
import com.oddlabs.tt.gui.FormData;
import com.oddlabs.tt.gui.GUIObject;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.Group;
import com.oddlabs.tt.gui.ScrollableGroup;
import com.oddlabs.tt.gui.HorizButton;
import com.oddlabs.tt.gui.Label;
import com.oddlabs.tt.gui.MouseButton;
import com.oddlabs.tt.gui.Origin;
import com.oddlabs.tt.gui.Panel;
import com.oddlabs.tt.gui.PulldownButton;
import com.oddlabs.tt.gui.PulldownItem;
import com.oddlabs.tt.gui.PulldownMenu;
import com.oddlabs.tt.gui.Skin;
import com.oddlabs.tt.gui.TextBox;
import com.oddlabs.tt.guievent.EnterListener;
import com.oddlabs.tt.guievent.ItemChosenListener;
import com.oddlabs.tt.guievent.MouseClickListener;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.net.ChatCommand;
import com.oddlabs.tt.net.ChatListener;
import com.oddlabs.tt.net.ChatMessage;
import com.oddlabs.tt.net.Client;
import com.oddlabs.tt.net.ConfigurationListener;
import com.oddlabs.tt.net.GameNetwork;
import com.oddlabs.tt.net.Network;
import com.oddlabs.tt.net.PlayerSlot;
import com.oddlabs.tt.player.PlayerInfo;
import com.oddlabs.tt.resource.WorldGenerator;
import com.oddlabs.tt.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.oddlabs.tt.gui.Placement.BOTTOM_LEFT;
import static com.oddlabs.tt.gui.Placement.BOTTOM_MID;
import static com.oddlabs.tt.gui.Placement.BOTTOM_RIGHT;
import static com.oddlabs.tt.gui.Placement.LEFT_MID;
import static com.oddlabs.tt.gui.Placement.RIGHT_MID;
import static com.oddlabs.tt.gui.Placement.TOP_RIGHT;

public final class GameMenu extends Panel implements ConfigurationListener, ChatListener {
    private static final ResourceBundle bundle = ResourceBundle.getBundle(GameMenu.class.getName());

    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private static final int OPEN_INDEX = 0;
    private static final int CLOSED_INDEX = 1;
    private static final int COMPUTER_EASY_INDEX = 2;
    private static final int COMPUTER_NORMAL_INDEX = 3;
    private static final int COMPUTER_HARD_INDEX = 4;

    private static final int SEND_BUTTON_WIDTH = 60;

    private static final int RATING_WIDTH = 80;
    private static final int DEFAULT_PLAYER_COUNT = 6;

    private final PulldownButton<Void> @NonNull [] slot_buttons;
    private final PulldownButton<Void> @NonNull [] race_buttons;
    private final PulldownButton<Void> @NonNull [] team_buttons;
    private final Label @NonNull [] ratings;
    private final @NonNull Label chat_info;
    private final @NonNull TextBox chat_box;
    private final @NonNull EditLine chat_line;
    private final Diode @NonNull [] ready_marks;
    private final @NonNull HorizButton ready_button;
    private final @NonNull HorizButton start_button;
    private final SelectGameMenu owner;
    private final GUIRoot gui_root;
    private final int local_player_slot;
    private final boolean rated;
    private final @NonNull Game game;
    private final @NonNull GameNetwork game_network;
    private @NonNull SortedSet<String> human_names = new TreeSet<>();

    private boolean updating;
    private boolean ready;

    @SuppressWarnings("unchecked")
    public GameMenu(@NonNull GameNetwork game_network, GUIRoot gui_root, SelectGameMenu owner, @NonNull Game game,
            WorldGenerator generator, int player_slot, int compare_width, int compare_height, int button_width,
            int player_count) {
        super(i18n("game_caption"));
        this.game_network = game_network;
        this.owner = owner;
        this.gui_root = gui_root;
        this.local_player_slot = player_slot;
        this.rated = game.isRated();
        this.game = game;

        String tag = rated ? i18n("rated") + " " : "";
        Label game_name_label = new Label(i18n("game") + " " + tag + game.getName(), Skin.getSkin().getHeadlineFont());

        slot_buttons = (PulldownButton<Void>[]) new PulldownButton[player_count];
        race_buttons = (PulldownButton<Void>[]) new PulldownButton[player_count];
        team_buttons = (PulldownButton<Void>[]) new PulldownButton[player_count];
        ready_marks = new Diode[player_count];
        ratings = new Label[player_count];
        Group player_group = player_count > DEFAULT_PLAYER_COUNT ? new ScrollableGroup(170, 64) : new Group();
        GUIObject previous = null;
        for (int i = 0; i < player_count; i++) {
            previous = createPlayerPulldown(gui_root, player_group, previous, slot_buttons, race_buttons, team_buttons,
                    ready_marks, ratings, i, player_count);
        }
        player_group.compileCanvas();
        addChild(player_group);

        Box pdata = Skin.getSkin().getPanelData().box();
        FormData fdata = Skin.getSkin().getFormData();

        int width = compare_width - pdata.getLeftOffset() - pdata.getRightOffset();
        chat_info = new Label(i18n("chat"), Skin.getSkin().getEditFont(), width);
        Group chat_line_group = new Group();
        chat_line = new EditLine(width - SEND_BUTTON_WIDTH - fdata.objectSpacing(), 100);
        var send_button = new HorizButton(i18n("send"), SEND_BUTTON_WIDTH);
        send_button.addMouseClickListener(new SendListener());
        chat_line_group.addChild(chat_line);
        chat_line.place();
        chat_line_group.addChild(send_button);
        send_button.place(chat_line, RIGHT_MID);
        chat_line_group.compileCanvas();
        addChild(chat_line_group);

        chat_line.addEnterListener(new ChatListener());
        addChild(game_name_label);
        addChild(chat_info);

        start_button = new HorizButton(i18n("start"), button_width);
        if (local_player_slot == 0) {
            addChild(start_button);
            start_button.addMouseClickListener(new StartListener());
        }
        int height = compare_height - pdata.getTopOffset() - pdata.getBottomOffset() - chat_info.getHeight() - chat_line.getHeight() - game_name_label.getHeight() - player_group.getHeight() - start_button.getHeight() - 5 * fdata.objectSpacing();
        chat_box = new TextBox(width, height, Skin.getSkin().getEditFont(), Integer.MAX_VALUE);
        addChild(chat_box);
        ready_button = new HorizButton(i18n("ready"), button_width);
        addChild(ready_button);
        ready_button.addMouseClickListener(new ReadyListener());
        HorizButton cancel_button = new HorizButton(i18n("cancel"), button_width);
        addChild(cancel_button);
        cancel_button.addMouseClickListener(new CancelButtonListener());
        HorizButton info_button = new HorizButton(i18n("info"), button_width);
        addChild(info_button);
        info_button.addMouseClickListener(new InfoButtonListener());

        game_name_label.place();
        player_group.place(game_name_label, BOTTOM_LEFT);
        chat_info.place(player_group, BOTTOM_LEFT);
        chat_box.place(chat_info, BOTTOM_LEFT);
        chat_line_group.place(chat_box, BOTTOM_LEFT);
        cancel_button.place(chat_line_group, BOTTOM_RIGHT);
        info_button.place(chat_line_group, BOTTOM_LEFT);
        ready_button.place(cancel_button, LEFT_MID);
        if (local_player_slot == 0)
            start_button.place(ready_button, LEFT_MID);
        Font font = Skin.getSkin().getEditFont();
        if (rated) {
            Label rating = new Label(i18n("rating"), font, RATING_WIDTH, Origin.AT_END);
            addChild(rating);
            rating.place(player_group, TOP_RIGHT);
            if (player_count > DEFAULT_PLAYER_COUNT) {
                rating.setPos(rating.getX() - 80, rating.getY());
            }
        }
        compileCanvas();
    }

    private void adjustPlayerSlot(int player_slot) {
        if (updating || game_network.getClient() == null)
            return;
        PlayerSlot player = game_network.getClient().getPlayers()[player_slot];
        int index = slot_buttons[player_slot].getMenu().getChosenItemIndex();
        int race_index = race_buttons[player_slot].getMenu().getChosenItemIndex();
        int team_index = team_buttons[player_slot].getMenu().getChosenItemIndex();
        int difficulty_index = slot_buttons[player_slot].getMenu().getChosenItemIndex() - 1;
        boolean race_changed = player.getInfo() == null || race_index != player.getInfo().getRace();
        boolean team_changed = player.getInfo() == null || team_index != player.getInfo().getTeam();
        boolean ready_changed = ready != player.isReady();
        boolean difficulty_changed = player.getInfo() == null || player.getAIDifficulty() != difficulty_index;
        PulldownButton<?> slot_button = slot_buttons[player_slot];
        switch (index) {
            case OPEN_INDEX:
                if ((player.getType() != PlayerSlot.OPEN && player.getType() != PlayerSlot.HUMAN) || race_changed
                        || team_changed || ready_changed) {
                    if (player_slot == local_player_slot) {
                        int new_type = PlayerSlot.HUMAN;
                        game_network.getClient().getServerInterface().setPlayerSlot(player_slot, new_type, race_index,
                                team_index, ready, PlayerSlot.AI_NONE);
                    } else {
                        game_network.getClient().getServerInterface().resetSlotState(player_slot, true);
                    }
                }
                break;
            case CLOSED_INDEX:
                if (player.getType() != PlayerSlot.CLOSED || race_changed || team_changed) {
                    slot_button.getMenu().getItem(OPEN_INDEX).setLabelString(i18n("open"));
                    game_network.getClient().getServerInterface().resetSlotState(player_slot, false);
                }
                break;
            case COMPUTER_EASY_INDEX:
            case COMPUTER_NORMAL_INDEX:
            case COMPUTER_HARD_INDEX:
                assert !rated;
                boolean new_ai = player.getType() != PlayerSlot.AI;
                if (new_ai || race_changed || team_changed || difficulty_changed) {
                    slot_button.getMenu().getItem(OPEN_INDEX).setLabelString(i18n("open"));
                    if (new_ai) {
                        team_index = player_slot;
                        race_index = new Random(LocalEventQueue.getQueue().getHighPrecisionManager().getTick()).nextInt(
                                RacesResources.getNumRaces());
                    }
                    game_network.getClient().getServerInterface().setPlayerSlot(player_slot, PlayerSlot.AI, race_index,
                            team_index, true, difficulty_index);
                }
                break;
            default:
                throw new RuntimeException("Invalid item index");
        }
    }

    @Override
    public void connected(Client client, Game game, WorldGenerator generator, int player_slot, int player_count) {
        assert false;
    }

    @Override
    public void setFocus(@NonNull FocusDirection direction) {
        if (direction == FocusDirection.BACKWARD) {
            super.setFocus(direction);
        } else {
            chat_line.setFocus(direction);
        }
    }

    private int countHumans(PlayerSlot @NonNull [] players) {
        int result = 0;
        for (PlayerSlot player : players) {
            if (player.getType() == PlayerSlot.HUMAN) {
                result++;
            }
        }
        return result;
    }

    @Override
    public void setPlayers(PlayerSlot @NonNull [] players) {
        int num_humans = countHumans(players);
        int[] player_slots = new int[num_humans];
        int[] player_ratings = new int[num_humans];
        int[] player_teams = new int[num_humans];
        int human_index = 0;
        updating = true;
        SortedSet<String> new_human_names = new TreeSet<>();
        for (int i = 0; i < players.length; i++) {
            PlayerSlot player = players[i];
            PulldownButton<?> slot_button = slot_buttons[i];
            PulldownButton<?> race_button = race_buttons[i];
            PulldownButton<?> team_button = team_buttons[i];
            Diode ready_mark = ready_marks[i];
            ready_mark.setLit(player.isReady());
            race_button.getMenu().chooseItem(player.getInfo() != null ? player.getInfo().getRace() : 0);
            team_button.getMenu().chooseItem(player.getInfo() != null ? player.getInfo().getTeam() : 0);
            if (player.getType() != PlayerSlot.CLOSED) {
                slot_button.getMenu().getItem(OPEN_INDEX).setLabelString(i18n("open"));
                slot_button.getMenu().chooseItem(OPEN_INDEX);
            } else {
                slot_button.getMenu().chooseItem(CLOSED_INDEX);
            }
            race_button.setDisabled(true);
            team_button.setDisabled(true);
            if (player.getInfo() != null) {
                PlayerInfo player_info = player.getInfo();
                switch (player.getType()) {
                    case PlayerSlot.AI:
                        assert !rated;
                        slot_button.getMenu().chooseItem(player.getAIDifficulty() + 1);
                        race_button.setDisabled(!canControlSlot(i));
                        team_button.setDisabled(!canControlSlot(i));
                        break;
                    case PlayerSlot.HUMAN:
                        String player_name = player_info.getName();
                        new_human_names.add(player_name);
                        slot_button.getMenu().getItem(OPEN_INDEX).setLabelString(player_name);
                        slot_button.getMenu().chooseItem(OPEN_INDEX);
                        race_button.setDisabled(i != local_player_slot);
                        team_button.setDisabled(i != local_player_slot);
                        player_slots[human_index] = i;
                        player_ratings[human_index] = player.getRating();
                        player_teams[human_index] = player_info.getTeam();
                        human_index++;
                        break;
                    default:
                        throw new RuntimeException("Unknown Player type: " + player.getType());
                }
            }
        }
        if (rated)
            updateRatedLabels(player_slots, player_ratings, GameSession.calculateMatchPoints(player_ratings,
                    player_teams));
        Iterator<String> it = new_human_names.iterator();
        while (it.hasNext()) {
            String name = it.next();
            if (human_names.contains(name))
                human_names.remove(name);
            else
                playerJoined(name);
        }
        it = human_names.iterator();
        while (it.hasNext()) {
            String name = it.next();
            playerLeft(name);
        }
        human_names = new_human_names;
        setReady(players[local_player_slot].isReady());
        setStartEnable(players);
        updating = false;
    }

    private void updateRatedLabels(int @NonNull [] player_slots, int[] player_ratings, int[][] points) {
        for (Label rating : ratings) {
            rating.clear();
        }
        for (int i = 0; i < player_slots.length; i++) {
            int slot = player_slots[i];
            if (slot == local_player_slot) {
                int win = points[i][GameSession.WIN];
                int lose = points[i][GameSession.LOSE];
                String rating_change_message = i18n("rating_change_message", Integer.toString(win), Integer.toString(
                        -lose));
                chat_info.set(rating_change_message);

            }
            ratings[slot].set("" + player_ratings[i]);
        }
    }

    private boolean canControlSlot(int slot) {
        return local_player_slot == 0 || slot == local_player_slot;
    }

    private @NonNull GUIObject createPlayerPulldown(GUIRoot gui_root, @NonNull Group group,
            @Nullable GUIObject previous,
            @NonNull PulldownButton<?>[] slot_buttons,
            @NonNull PulldownButton<?>[] race_buttons,
            @NonNull PulldownButton<?>[] team_buttons,
            @NonNull Diode[] ready_marks,
            @NonNull Label[] ratings,
            int index,
            int num_players) {
        PulldownMenu<Void> pulldown_menu = new PulldownMenu<>();
        PulldownItem<Void> open_item = new PulldownItem<>(i18n("open"));
        PulldownItem<Void> closed_item = new PulldownItem<>(i18n("closed"));
        PulldownItem<Void> computer_easy_item = new PulldownItem<>(i18n("easy_ai"));
        PulldownItem<Void> computer_normal_item = new PulldownItem<>(i18n("normal_ai"));
        PulldownItem<Void> computer_hard_item = new PulldownItem<>(i18n("hard_ai"));
        pulldown_menu.addItem(open_item);
        pulldown_menu.addItem(closed_item);
        if (!rated) {
            pulldown_menu.addItem(computer_easy_item);
            pulldown_menu.addItem(computer_normal_item);
            pulldown_menu.addItem(computer_hard_item);
        }
        PulldownButton<?> pulldown_button = new PulldownButton<>(gui_root, pulldown_menu, CLOSED_INDEX, 150);
        slot_buttons[index] = pulldown_button;
        group.addChild(pulldown_button);
        if (previous != null)
            pulldown_button.place(previous, BOTTOM_MID);
        else
            pulldown_button.place();
        pulldown_menu.addItemChosenListener(new PlayerSlotListener(index));
        pulldown_button.setDisabled(local_player_slot != 0 || index == local_player_slot);

        PulldownMenu<Void> race_pulldown_menu = new PulldownMenu<>();
        for (int i = 0; i < RacesResources.getNumRaces(); i++) {
            PulldownItem<Void> race_item = new PulldownItem<>(RacesResources.getRaceName(i));
            race_pulldown_menu.addItem(race_item);
        }
        PulldownMenu<Void> team_pulldown_menu = new PulldownMenu<>();
        int num_teams = num_players;
        if (rated)
            num_teams = 2;
        for (int i = 0; i < num_teams; i++) {
            String team_str = i18n("team", Integer.toString(i + 1));
            PulldownItem<Void> race_item = new PulldownItem<>(team_str);
            team_pulldown_menu.addItem(race_item);
        }
        PulldownButton<?> race_pulldown_button = new PulldownButton<>(gui_root, race_pulldown_menu, 0, 115);
        PulldownButton<?> team_pulldown_button = new PulldownButton<>(gui_root, team_pulldown_menu, index % num_teams,
                115);
        race_buttons[index] = race_pulldown_button;
        team_buttons[index] = team_pulldown_button;
        group.addChild(race_pulldown_button);
        group.addChild(team_pulldown_button);
        race_pulldown_button.place(pulldown_button, RIGHT_MID);
        team_pulldown_button.place(race_pulldown_button, RIGHT_MID);
        race_pulldown_menu.addItemChosenListener(new PlayerSlotListener(index));
        team_pulldown_menu.addItemChosenListener(new PlayerSlotListener(index));
        race_pulldown_button.setDisabled(!canControlSlot(index));
        team_pulldown_button.setDisabled(!canControlSlot(index));

        Diode ready_mark = new Diode();
        ready_marks[index] = ready_mark;
        group.addChild(ready_mark);
        ready_mark.place(team_pulldown_button, RIGHT_MID);
        Font font = Skin.getSkin().getEditFont();
        ratings[index] = new Label("", font, RATING_WIDTH, Origin.AT_END);
        if (rated) {
            group.addChild(ratings[index]);
            ratings[index].place(ready_mark, RIGHT_MID);
        }
        String player_str = i18n("player", Integer.toString(index + 1));
        Label label = new Label(player_str, Skin.getSkin().getEditFont()).setColor(
                Settings.getSettings().team_colours[index]);
        group.addChild(label);
        label.place(pulldown_button, LEFT_MID);

        return pulldown_button;
    }

    @Override
    protected void doAdd() {
        super.doAdd();
        Network.getChatHub().addListener(this);
    }

    @Override
    protected void doRemove() {
        super.doRemove();
        Network.getChatHub().removeListener(this);
    }

    @Override
    public void connectionLost() {
        remove();
        owner.removeGameMenu();
        gui_root.addModalForm(new MessageForm(i18n("connection_lost")));
    }

    @Override
    public void gameStarted() {
//		owner.removeGameMenu();
        setDisabled(true);
    }

    private void finishChatAppend() {
        chat_box.setOffsetY(Integer.MAX_VALUE);
        getTab().updateNotify();
    }

    @Override
    public void chat(@NonNull ChatMessage message) {
        if (message.type() != ChatMessage.Type.GAME_MENU)
            return;
        if (!chat_box.isEmpty())
            chat_box.append("\n");

        chat_box.append(message.formatLong());
        finishChatAppend();
    }

    private void playerLeft(String name) {
        if (!chat_box.isEmpty())
            chat_box.append("\n");
        chat_box.append(i18n("left_game", name));
        finishChatAppend();
    }

    private void playerJoined(String name) {
        if (!chat_box.isEmpty())
            chat_box.append("\n");
        chat_box.append(i18n("joined_game", name));
        finishChatAppend();
    }

    private void setReady(boolean r) {
        if (r != ready) {
            ready = r;
            ready_button.setDisabled(ready);
            adjustPlayerSlot(local_player_slot);
        }
    }

    private void setStartEnable(PlayerSlot @NonNull [] players) {
        start_button.setDisabled(true);
        if (local_player_slot != 0)
            return;
        for (PlayerSlot player : players) {
            if (!player.isReady()) {
                return;
            }
        }
        start_button.setDisabled(false);
    }

    void cancel() {
        game_network.close();
        owner.removeGameMenu();
    }

    private final class InfoButtonListener implements MouseClickListener {
        @Override
        public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
            gui_root.addModalForm(new GameInfoForm(game));
        }
    }

    private final class CancelButtonListener implements MouseClickListener {
        @Override
        public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
            cancel();
        }
    }

    private final class ReadyListener implements MouseClickListener {
        @Override
        public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
            setReady(true);
        }
    }

    private static int getNumTeams(PlayerSlot @NonNull [] players) {
        Set<Integer> teams = new HashSet<>();
        for (PlayerSlot current : players) {
            if (current.getInfo() != null)
                teams.add(current.getInfo().getTeam());
        }
        return teams.size();
    }

    private final class StartListener implements MouseClickListener {
        @Override
        public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
            final int MIN_TEAMS = 2;
            int num_teams = getNumTeams(game_network.getClient().getPlayers());
            if (num_teams < MIN_TEAMS) {
                String err_msg = i18n("min_teams", Integer.toString(MIN_TEAMS));
                gui_root.addModalForm(new MessageForm(err_msg));
            } else {
                game_network.getClient().getServerInterface().startServer();
            }
        }
    }

    private final class PlayerSlotListener implements ItemChosenListener<Void> {
        private final int player_slot;

        public PlayerSlotListener(int player_slot) {
            this.player_slot = player_slot;
        }

        @Override
        public void itemChosen(@NonNull PulldownMenu<Void> menu, int item_index) {
            setReady(false);
            adjustPlayerSlot(player_slot);
        }
    }

    private final class ChatListener implements EnterListener {
        @Override
        public void enterPressed(@NonNull CharSequence text) {
            String chat = text.toString();
            if (!chat.isEmpty()) {
                chat_line.clear();
                if (!ChatCommand.filterCommand(gui_root.getInfoPrinter(), chat))
                    game_network.getClient().getServerInterface().chat(chat);
            }
        }
    }

    private final class SendListener implements MouseClickListener {
        @Override
        public void mouseClicked(@NonNull MouseButton button, int x, int y, int clicks) {
            chat_line.enterPressedAll();
        }
    }

}
