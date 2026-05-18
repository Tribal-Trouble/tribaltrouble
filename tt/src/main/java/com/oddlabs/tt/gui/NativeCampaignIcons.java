package com.oddlabs.tt.gui;

import com.oddlabs.tt.render.Texture;
import org.jspecify.annotations.NonNull;
import org.w3c.dom.Node;

public final class NativeCampaignIcons implements CampaignIcons {
    private static final int NUM_ISLANDS = 8;

    private static final NativeCampaignIcons ICONS = new NativeCampaignIcons("/gui/native_campaign.xml");

    private final @NonNull IconQuad map;
    private final @NonNull MapIslandData[] islands = new MapIslandData[NUM_ISLANDS];
    private final @NonNull IconQuad @NonNull [] flags = new IconQuad[3];
    private final @NonNull IconQuad[] boats = new IconQuad[3];
    private final @NonNull GUIIcon[] hidden = new GUIIcon[1];
    private final @NonNull IconQuad[] faces = new IconQuad[9];
    private final int offset_x;
    private final int offset_y;
    private final int width;
    private final int height;

    public static NativeCampaignIcons getIcons() {
        return ICONS;
    }

    private NativeCampaignIcons(@NonNull String xml_file) {
        Node root = Icons.loadFile(xml_file, new GUIErrorHandler());
        Texture atlas = Icons.loadTexture(root);

        flags[0] = Icons.getNamedIconQuad(root, "flag0", atlas);
        flags[1] = Icons.getNamedIconQuad(root, "flag1", atlas);
        flags[2] = Icons.getNamedIconQuad(root, "flag2", atlas);
        boats[0] = Icons.getNamedIconQuad(root, "boat0", atlas);
        boats[1] = Icons.getNamedIconQuad(root, "boat1", atlas);
        boats[2] = Icons.getNamedIconQuad(root, "boat2", atlas);
        hidden[0] = getNamedGUIIcon(root, "hidden0", atlas);
        faces[0] = Icons.getNamedIconQuad(root, "face0", atlas);
        faces[1] = Icons.getNamedIconQuad(root, "face1", atlas);
        faces[2] = Icons.getNamedIconQuad(root, "face2", atlas);
        faces[3] = Icons.getNamedIconQuad(root, "face3", atlas);
        faces[4] = Icons.getNamedIconQuad(root, "face4", atlas);
        faces[5] = Icons.getNamedIconQuad(root, "face5", atlas);
        faces[6] = Icons.getNamedIconQuad(root, "face6", atlas);
        faces[7] = Icons.getNamedIconQuad(root, "face7", atlas);
        faces[8] = Icons.getNamedIconQuad(root, "face8", atlas);

        map = Icons.getNamedIconQuad(root, "map", atlas);
        for (int i = 0; i < NUM_ISLANDS; i++) {
            islands[i] = loadMapIslandData(root, "island" + i, atlas);
        }

        Node map_node = Icons.getNodeByName("map", root);
        offset_x = Icons.getInt(map_node, "offset_x");
        offset_y = Icons.getInt(map_node, "offset_y");
        width = Icons.getInt(map_node, "width");
        height = Icons.getInt(map_node, "height");
    }

    private @NonNull MapIslandData loadMapIslandData(@NonNull Node root, @NonNull String name,
            @NonNull Texture texture) {
        Node node = Icons.getNodeByName(name, root);
        ModeIconQuads quads = Icons.getNamedIconQuads(node, "island", texture);
        Node n = Icons.getNodeByName("island", node);
        int x = Icons.getInt(n, "x");
        int y = texture.getHeight() - Icons.getInt(n, "y");
        int pin_index = Icons.getInt(n, "pin_index");
        int pin_x = Icons.getInt(n, "pin_x");
        int pin_y = texture.getHeight() - Icons.getInt(n, "pin_y");
        return new MapIslandData(quads, x, y, flags[pin_index], boats[pin_index], pin_x, pin_y);
    }

    private @NonNull GUIIcon getNamedGUIIcon(@NonNull Node root, @NonNull String name, @NonNull Texture texture) {
        IconQuad temp = Icons.getNamedIconQuad(root, name, texture);
        Node n = Icons.getNodeByName(name, root);
        int x = Icons.getInt(n, "x");
        int y = texture.getHeight() - Icons.getInt(n, "y");
        GUIIcon gui_icon = new GUIIcon(temp);
        gui_icon.setPos(x, y);
        return gui_icon;
    }

    @Override
    public @NonNull GUIIcon @NonNull [] getHiddenRoutes() {
        return hidden;
    }

    @Override
    public @NonNull IconQuad @NonNull [] getFaces() {
        return faces;
    }

    @Override
    public @NonNull IconQuad getMap() {
        return map;
    }

    @Override
    public int getNumIslands() {
        return islands.length;
    }

    public int getOffsetX() {
        return offset_x;
    }

    public int getOffsetY() {
        return offset_y;
    }

    public int getInternalWidth() {
        return width;
    }

    public int getInternalHeight() {
        return height;
    }

    @Override
    public @NonNull MapIslandData getMapIslandData(int i) {
        return islands[i];
    }
}
