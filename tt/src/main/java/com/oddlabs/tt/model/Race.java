package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.Audio;
import com.oddlabs.tt.gui.RaceIcons;
import com.oddlabs.tt.model.weapon.MagicFactory;
import com.oddlabs.tt.player.ChieftainAI;
import com.oddlabs.tt.render.SpriteKey;
import org.jspecify.annotations.NonNull;

public final class Race {
    public static final int BUILDING_QUARTERS = 0;
    public static final int BUILDING_ARMORY = 1;
    public static final int BUILDING_TOWER = 2;
    public static final int BUILDING_SHIP = 3;
    public static final int NUM_BUILDINGS = 4;

    public static final int UNIT_WARRIOR_ROCK = 0;
    public static final int UNIT_WARRIOR_IRON = 1;
    public static final int UNIT_WARRIOR_RUBBER = 2;
    public static final int UNIT_PEON = 3;
    public static final int UNIT_CHIEFTAIN = 4;

    private final @NonNull BuildingTemplate[] buildings = new BuildingTemplate[NUM_BUILDINGS];
    private final @NonNull UnitTemplate[] units = new UnitTemplate[5];
    private final @NonNull SpriteKey rally_point;
    private final @NonNull RaceIcons icons;
    private final @NonNull Audio attack_notification;
    private final @NonNull Audio building_notification;
    private final @NonNull MagicFactory @NonNull [] magic_factory;
    private final @NonNull ChieftainAI chieftain_ai;
    private final @NonNull String music_path;

    public Race(
            @NonNull BuildingTemplate quarters,
            @NonNull BuildingTemplate armory,
            @NonNull BuildingTemplate tower,
            @NonNull BuildingTemplate ship,
            @NonNull UnitTemplate warrior_rock,
            @NonNull UnitTemplate warrior_iron,
            @NonNull UnitTemplate warrior_rubber,
            @NonNull UnitTemplate peon,
            @NonNull UnitTemplate chieftain,
            @NonNull SpriteKey rally_point,
            @NonNull RaceIcons icons,
            @NonNull Audio attack_notification,
            @NonNull Audio building_notification,
            @NonNull MagicFactory @NonNull [] magic_factory,
            @NonNull ChieftainAI chieftain_ai,
            @NonNull String music_path) {
        buildings[BUILDING_QUARTERS] = quarters;
        buildings[BUILDING_ARMORY] = armory;
        buildings[BUILDING_TOWER] = tower;
        buildings[BUILDING_SHIP] = ship;
        for (int i = 0; i < buildings.length; i++) {
            assert buildings[i].getTemplateID() == i;
        }
        units[UNIT_WARRIOR_ROCK] = warrior_rock;
        units[UNIT_WARRIOR_IRON] = warrior_iron;
        units[UNIT_WARRIOR_RUBBER] = warrior_rubber;
        units[UNIT_PEON] = peon;
        units[UNIT_CHIEFTAIN] = chieftain;
        this.rally_point = rally_point;
        this.icons = icons;
        this.attack_notification = attack_notification;
        this.building_notification = building_notification;
        this.magic_factory = magic_factory;
        this.chieftain_ai = chieftain_ai;
        this.music_path = music_path;
    }

    public @NonNull BuildingTemplate getBuildingTemplate(int index) {
        return buildings[index];
    }

    public @NonNull UnitTemplate getUnitTemplate(int index) {
        return units[index];
    }

    public @NonNull SpriteKey getRallyPoint() {
        return rally_point;
    }

    public @NonNull RaceIcons getIcons() {
        return icons;
    }

    public @NonNull Audio getAttackNotificationAudio() {
        return attack_notification;
    }

    public @NonNull Audio getBuildingNotificationAudio() {
        return building_notification;
    }

    public @NonNull MagicFactory getMagicFactory(int i) {
        return magic_factory[i];
    }

    public @NonNull ChieftainAI getChieftainAI() {
        return chieftain_ai;
    }

    public @NonNull String getMusicPath() {
        return music_path;
    }
}
