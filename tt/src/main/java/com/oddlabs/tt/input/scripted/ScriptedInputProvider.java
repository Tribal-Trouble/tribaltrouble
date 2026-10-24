package com.oddlabs.tt.input.scripted;

import com.oddlabs.tt.animation.AnimationManager;
import com.oddlabs.tt.event.LocalEventQueue;
import com.oddlabs.tt.gui.GUIRoot;
import com.oddlabs.tt.gui.GUITreeDump;
import com.oddlabs.tt.gui.PasswordLine;
import com.oddlabs.tt.gui.TextField;
import com.oddlabs.tt.input.InputProvider;
import com.oddlabs.tt.input.Key;
import com.oddlabs.tt.input.LWJGL3InputProvider;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.util.GLUtils;
import com.oddlabs.tt.window.LWJGL3Window;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * Wraps the real input provider and merges in events sent over a loopback socket, one line per command, so a script
 * or agent can drive the game without touching game code. Enabled by the {@code com.oddlabs.tt.input.port} system
 * property. Commands run on the game thread at the next poll and are answered with {@code ok ...} or {@code err ...}:
 * <pre>
 * move X Y cursor to framebuffer pixel (top-left origin, same space as screenshots)
 * down|up [button] press or release left (default), right or middle
 * click [button] press now, release at the next poll
 * key NAME [down|up] Key enum name (RETURN, ESCAPE, A ...); default press then release
 * type TEXT character input, one character per poll
 * shot [PATH] screenshot; replies with the file written
 * tree GUI tree, one object per line in framebuffer pixels, terminated by END
 * find TEXT framebuffer centre of the last widget whose label matches TEXT
 * press TEXT move to that widget and click it; rpress TEXT right-clicks it
 * choose LABEL=ITEM open the pulldown on LABEL's row and pick ITEM
 * edit LABEL=TEXT set the edit field on the row of LABEL (or the field showing LABEL) to TEXT
 * state players, camera, and every unit and building with world and screen coordinates, terminated by END
 * sites [QUARTERS|ARMORY|TOWER|SHIP] [radius] legal placement cells near the selection with screen coordinates,
 * terminated by END
 * tick current simulation tick (4 ms high-precision ticks)
 * warp MILLIS fast-forward the simulation by MILLIS of game time; answers once it has caught up
 * wait tick N | wait delegate NAME | wait text TEXT [SECONDS] answers once the condition holds, or
 * "err timeout" after SECONDS (default 300)
 * scroll N wheel steps at the cursor (negative for down), one per poll
 * ping
 * </pre>
 */
public final class ScriptedInputProvider implements InputProvider<Long> {
    private static final Logger logger = Logger.getLogger(ScriptedInputProvider.class.getName());
    private static final long WARP_CHUNK_MILLIS = AnimationManager.MAX_STEP_MILLIS - 5000;

    private record MouseEvent(int button, boolean state, int x, int y, int dWheel) {
    }

    private record KeyEvent(int key, boolean down, char character) {
    }

    // A command that cannot finish in this poll waits in the deferred queue until its deadline, then answers
    // "err timeout" so a client that gave up never leaves the queue wedged. wait commands take the deadline from
    // their optional trailing SECONDS argument; everything else gets a generous default.
    private static final long DEFAULT_DEADLINE_SECONDS = 300;

    private record Command(@NonNull String line, @NonNull CompletableFuture<String> reply, long deadline_nanos) {
        Command(@NonNull String line, @NonNull CompletableFuture<String> reply) {
            this(line, reply, System.nanoTime() + secondsFor(line) * 1_000_000_000L);
        }

        private static long secondsFor(@NonNull String line) {
            String[] parts = line.split("\\s+");
            if (parts.length >= 4 && parts[0].equalsIgnoreCase("wait")) {
                try {
                    return Long.parseLong(parts[parts.length - 1]);
                } catch (NumberFormatException _) {
                    return DEFAULT_DEADLINE_SECONDS;
                }
            }
            return DEFAULT_DEADLINE_SECONDS;
        }
    }

    private final @NonNull LWJGL3InputProvider delegate;
    private final @NonNull LWJGL3Window window;
    private final @NonNull ConcurrentLinkedQueue<Command> commands = new ConcurrentLinkedQueue<>();
    private final @NonNull Deque<Command> deferred = new ArrayDeque<>();
    private final @NonNull Deque<MouseEvent> mouseEvents = new ArrayDeque<>();
    private final @NonNull Deque<KeyEvent> keyEvents = new ArrayDeque<>();
    private final @NonNull Set<Integer> keysDown = new HashSet<>();
    private @Nullable MouseEvent currentMouse;
    private @Nullable KeyEvent currentKey;
    private boolean positioned;
    private int x;
    private int y;
    private @Nullable GUIRoot root;
    private long warp_remaining;
    private long warp_chunk_end;
    private int scroll_remaining;

    public ScriptedInputProvider(@NonNull LWJGL3InputProvider delegate, @NonNull LWJGL3Window window, int port) {
        this.delegate = delegate;
        this.window = window;
        Thread thread = new Thread(() -> serve(port), "scripted-input");
        thread.setDaemon(true);
        thread.start();
    }

    public @NonNull LWJGL3InputProvider getDelegate() {
        return delegate;
    }

    public void setGuiRoot(@NonNull GUIRoot root) {
        this.root = root;
    }

    private static int currentTick() {
        return LocalEventQueue.getQueue().getHighPrecisionManager().getTick();
    }

    private void serve(int port) {
        try (ServerSocket server = new ServerSocket(port, 1, InetAddress.getLoopbackAddress())) {
            logger.info("Scripted input listening on " + server.getLocalSocketAddress());
            while (true) {
                try (Socket socket = server.accept(); BufferedReader in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8)); PrintWriter out = new PrintWriter(socket.getOutputStream(), true,
                                StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = in.readLine()) != null) {
                        Command command = new Command(line.strip(), new CompletableFuture<>());
                        commands.add(command);
                        out.println(command.reply().join());
                    }
                } catch (IOException e) {
                    logger.warning("Scripted input client error: " + e);
                }
            }
        } catch (IOException e) {
            logger.severe("Scripted input server failed: " + e);
        }
    }

    // Runs on the game thread once per poll. One deferred command (a release or the next typed character) is
    // applied per poll so the GUI sees press and release in separate frames.
    private void applyCommands() {
        if (!deferred.isEmpty()) {
            Command command = deferred.poll();
            if (System.nanoTime() > command.deadline_nanos()) {
                command.reply().complete("err timeout");
                return;
            }
            String reply = execute(command.line(), true);
            if (reply == null) {
                deferred.addFirst(command);
            } else {
                command.reply().complete(reply);
            }
            return;
        }
        Command command;
        while ((command = commands.poll()) != null) {
            String reply = execute(command.line(), false);
            if (reply == null) {
                deferred.add(command);
                return;
            }
            command.reply().complete(reply);
        }
    }

    private @Nullable String execute(@NonNull String line, boolean second_pass) {
        String[] parts = line.split("\\s+", 3);
        String verb = parts.length > 0 ? parts[0].toLowerCase(Locale.ROOT) : "";
        try {
            switch (verb) {
                case "ping":
                    return "ok pong";
                case "move":
                    moveTo(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                    return "ok " + x + " " + y;
                case "down":
                case "up":
                    queueButton(button(parts), verb.equals("down"));
                    return "ok";
                case "click":
                    if (second_pass) {
                        queueButton(button(parts), false);
                        return "ok";
                    }
                    queueButton(button(parts), true);
                    return null;
                case "key": {
                    Key key = Key.valueOf(parts[1].toUpperCase(Locale.ROOT));
                    String action = parts.length > 2 ? parts[2].toLowerCase(Locale.ROOT) : "press";
                    if (action.equals("down") || (action.equals("press") && !second_pass)) {
                        queueKey(key.getGlfwCode(), true, (char) 0);
                        return action.equals("down") ? "ok" : null;
                    }
                    queueKey(key.getGlfwCode(), false, (char) 0);
                    return "ok";
                }
                case "type": {
                    String text = line.length() > 5 ? line.substring(5) : "";
                    if (text.isEmpty()) {
                        return "ok";
                    }
                    char c = text.charAt(0);
                    queueKey(GLFW.GLFW_KEY_UNKNOWN, true, c);
                    queueKey(GLFW.GLFW_KEY_UNKNOWN, false, (char) 0);
                    if (text.length() > 1) {
                        // Re-queue the rest so each character lands in its own poll.
                        deferred.add(new Command("type " + text.substring(1), new CompletableFuture<>()));
                    }
                    return "ok";
                }
                case "shot": {
                    String path = parts.length > 1 ? parts[1] : "";
                    return "ok " + GLUtils.takeScreenshot(path);
                }
                case "tree":
                    if (root == null) {
                        return "err no gui root yet";
                    }
                    return "ok\n" + GUITreeDump.dump(root, window.getHeight()) + "END";
                case "find": {
                    if (root == null) {
                        return "err no gui root yet";
                    }
                    int[] at = GUITreeDump.find(root, window.getHeight(), rest(line, verb));
                    return at == null ? "err not found: " + rest(line, verb) : "ok " + at[0] + " " + at[1];
                }
                case "press":
                case "rpress": {
                    int mouse_button = verb.equals(
                            "rpress") ? GLFW.GLFW_MOUSE_BUTTON_RIGHT : GLFW.GLFW_MOUSE_BUTTON_LEFT;
                    if (second_pass) {
                        queueButton(mouse_button, false);
                        return "ok";
                    }
                    if (root == null) {
                        return "err no gui root yet";
                    }
                    int[] at = GUITreeDump.find(root, window.getHeight(), rest(line, verb));
                    if (at == null) {
                        return "err not found: " + rest(line, verb);
                    }
                    moveTo(at[0], at[1]);
                    queueButton(mouse_button, true);
                    return null;
                }
                case "choose": {
                    // choose LABEL=ITEM: open the pulldown on LABEL's row, then press ITEM at the following polls.
                    String[] pair = rest(line, verb).split("=", 2);
                    if (pair.length != 2) {
                        return "err usage: choose LABEL=ITEM";
                    }
                    if (second_pass) {
                        queueButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, false);
                        commands.add(new Command("press " + pair[1], new CompletableFuture<>()));
                        return "ok";
                    }
                    if (root == null) {
                        return "err no gui root yet";
                    }
                    int[] at = GUITreeDump.findPulldown(root, window.getHeight(), pair[0]);
                    if (at == null) {
                        return "err no pulldown for: " + pair[0];
                    }
                    moveTo(at[0], at[1]);
                    queueButton(GLFW.GLFW_MOUSE_BUTTON_LEFT, true);
                    return null;
                }
                case "edit": {
                    if (root == null) {
                        return "err no gui root yet";
                    }
                    String[] pair = rest(line, verb).split("=", 2);
                    if (pair.length != 2) {
                        return "err usage: edit LABEL=TEXT";
                    }
                    TextField field = GUITreeDump.findField(root, window.getHeight(), pair[0]);
                    if (field == null) {
                        return "err no edit field for: " + pair[0];
                    }
                    if (field instanceof PasswordLine password) {
                        password.setPassword(pair[1]);
                    } else {
                        field.clear();
                        field.append(pair[1]);
                    }
                    return "ok " + field.getContents();
                }
                case "scroll": {
                    // scroll N: N wheel steps at the cursor, one per poll because the GUI collapses a poll's
                    // wheel events into a single step.
                    if (!second_pass) {
                        scroll_remaining = Integer.parseInt(parts[1]);
                    }
                    if (scroll_remaining == 0) {
                        return "ok";
                    }
                    int step = scroll_remaining > 0 ? 1 : -1;
                    scroll_remaining -= step;
                    if (!positioned) {
                        x = delegate.getMouseX();
                        y = delegate.getMouseY();
                        positioned = true;
                    }
                    mouseEvents.add(new MouseEvent(-1, false, x, y, step * 120));
                    return scroll_remaining == 0 ? "ok" : null;
                }
                case "tick":
                    return "ok tick " + currentTick();
                case "warp": {
                    // warp MILLIS: pushes the game clock forward in chunks the loop will accept (it drops any frame
                    // step over MAX_STEP_MILLIS) and answers once the simulation has caught up.
                    if (!second_pass) {
                        warp_remaining = Long.parseLong(parts[1]);
                        warp_chunk_end = currentTick();
                    }
                    if (currentTick() < warp_chunk_end) {
                        return null;
                    }
                    if (warp_remaining <= 0) {
                        return "ok tick " + currentTick();
                    }
                    long chunk = Math.min(warp_remaining, WARP_CHUNK_MILLIS);
                    warp_remaining -= chunk;
                    warp_chunk_end = currentTick() + chunk / AnimationManager.ANIMATION_MILLISECONDS_PER_PRECISION_TICK;
                    AnimationManager.warpTime(chunk);
                    return null;
                }
                case "wait": {
                    // wait tick N | wait delegate NAME | wait text TEXT: answered once the condition holds.
                    String what = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "tick";
                    String value = parts.length > 2 ? parts[2] : parts[1];
                    if (parts.length > 2) {
                        String[] words = parts[2].split("\\s+");
                        boolean trailing_seconds = words.length > 1 && words[words.length - 1].matches("\\d+");
                        value = trailing_seconds ? String.join(" ", java.util.Arrays.copyOf(words,
                                words.length - 1)) : parts[2];
                    }
                    if (what.equals("delegate")) {
                        String name = root == null ? "" : root.getDelegate().getClass().getSimpleName();
                        return name.equalsIgnoreCase(value) ? "ok delegate " + name : null;
                    }
                    if (what.equals("text")) {
                        return root != null && GUITreeDump.has(root, window.getHeight(), value) ? "ok" : null;
                    }
                    long target = Long.parseLong(value);
                    return currentTick() >= target ? "ok tick " + currentTick() : null;
                }
                case "sites": {
                    if (root == null) {
                        return "err no gui root yet";
                    }
                    int building = switch (parts.length > 1 ? parts[1].toUpperCase(Locale.ROOT) : "QUARTERS") {
                        case "ARMORY" -> Race.BUILDING_ARMORY;
                        case "TOWER" -> Race.BUILDING_TOWER;
                        case "SHIP" -> Race.BUILDING_SHIP;
                        default -> Race.BUILDING_QUARTERS;
                    };
                    int radius = parts.length > 2 ? Integer.parseInt(parts[2]) : 20;
                    String dump = GameStateDump.sites(root, building, radius, window.getWidth(),
                            window.getHeight());
                    return dump.startsWith("err") ? dump.strip() : dump + "END";
                }
                case "state": {
                    if (root == null) {
                        return "err no gui root yet";
                    }
                    String dump = GameStateDump.dump(root, window.getWidth(), window.getHeight());
                    return dump.startsWith("err") ? dump.strip() : dump + "END";
                }
                default:
                    return "err unknown command: " + line;
            }
        } catch (RuntimeException e) {
            return "err " + e;
        }
    }

    private static @NonNull String rest(@NonNull String line, @NonNull String verb) {
        return line.substring(verb.length()).strip();
    }

    private static int button(@NonNull String @NonNull [] parts) {
        if (parts.length < 2) {
            return GLFW.GLFW_MOUSE_BUTTON_LEFT;
        }
        return switch (parts[1].toLowerCase(Locale.ROOT)) {
            case "right" -> GLFW.GLFW_MOUSE_BUTTON_RIGHT;
            case "middle" -> GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
            default -> GLFW.GLFW_MOUSE_BUTTON_LEFT;
        };
    }

    // Screenshot space is framebuffer pixels with a top-left origin; the engine wants bottom-left.
    private void moveTo(int px, int py) {
        x = px;
        y = window.getHeight() - py - 1;
        positioned = true;
        mouseEvents.add(new MouseEvent(-1, false, x, y, 0));
    }

    private void queueButton(int button, boolean down) {
        if (!positioned) {
            x = delegate.getMouseX();
            y = delegate.getMouseY();
            positioned = true;
        }
        mouseEvents.add(new MouseEvent(button, down, x, y, 0));
    }

    private void queueKey(int code, boolean down, char character) {
        if (down) {
            keysDown.add(code);
        } else {
            keysDown.remove(code);
        }
        keyEvents.add(new KeyEvent(code, down, character));
    }

    @Override
    public void pollKeyboard() {
        delegate.pollKeyboard();
    }

    @Override
    public boolean nextKeyboardEvent() {
        currentKey = keyEvents.poll();
        return currentKey != null || delegate.nextKeyboardEvent();
    }

    @Override
    public int getEventKey() {
        return currentKey != null ? currentKey.key() : delegate.getEventKey();
    }

    @Override
    public boolean getEventKeyState() {
        return currentKey != null ? currentKey.down() : delegate.getEventKeyState();
    }

    @Override
    public int getEventKeyMods() {
        return currentKey != null ? 0 : delegate.getEventKeyMods();
    }

    @Override
    public char getEventCharacter() {
        return currentKey != null ? currentKey.character() : delegate.getEventCharacter();
    }

    @Override
    public boolean isRepeatEvent() {
        return currentKey == null && delegate.isRepeatEvent();
    }

    @Override
    public boolean isKeyDown(int keyCode) {
        return keysDown.contains(keyCode) || delegate.isKeyDown(keyCode);
    }

    @Override
    public void pollMouse() {
        delegate.pollMouse();
        applyCommands();
    }

    @Override
    public boolean nextMouseEvent() {
        currentMouse = mouseEvents.poll();
        return currentMouse != null || delegate.nextMouseEvent();
    }

    @Override
    public int getEventButton() {
        return currentMouse != null ? currentMouse.button() : delegate.getEventButton();
    }

    @Override
    public boolean getEventButtonState() {
        return currentMouse != null ? currentMouse.state() : delegate.getEventButtonState();
    }

    @Override
    public int getEventDWheel() {
        return currentMouse != null ? currentMouse.dWheel() : delegate.getEventDWheel();
    }

    @Override
    public int getEventX() {
        return currentMouse != null ? currentMouse.x() : delegate.getEventX();
    }

    @Override
    public int getEventY() {
        return currentMouse != null ? currentMouse.y() : delegate.getEventY();
    }

    @Override
    public int getMouseX() {
        return positioned ? x : delegate.getMouseX();
    }

    @Override
    public int getMouseY() {
        return positioned ? y : delegate.getMouseY();
    }

    @Override
    public boolean isButtonDown(int button) {
        return delegate.isButtonDown(button);
    }

    @Override
    public void setCursorPosition(int x, int y) {
        delegate.setCursorPosition(x, y);
    }

    @Override
    public void setGrabbed(boolean grabbed) {
        delegate.setGrabbed(grabbed);
    }

    @Override
    public boolean isGrabbed() {
        return delegate.isGrabbed();
    }

    @Override
    public boolean isCursorInWindow() {
        return positioned || delegate.isCursorInWindow();
    }

    @Override
    public void setNativeCursor(@Nullable Long cursor) {
        delegate.setNativeCursor(cursor);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
