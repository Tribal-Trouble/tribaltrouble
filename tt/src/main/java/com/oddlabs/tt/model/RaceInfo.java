package com.oddlabs.tt.model;

import com.oddlabs.tt.audio.AudioParameters;
import com.oddlabs.tt.model.weapon.MagicFactory;
import com.oddlabs.tt.player.ChieftainAI;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.tt.resource.AudioFile;
import org.jspecify.annotations.NonNull;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Info for a playable race in the game (e.g., Natives or Vikings).
 * Defines the templates for buildings, units, and magical abilities available to the race.
 */
public final class RaceInfo {
    private final @NonNull Race race;
    private final @NonNull EnumMap<BuildingType, BuildingTemplate> buildings = new EnumMap<>(BuildingType.class);
    private final @NonNull EnumMap<UnitType, UnitTemplate> units = new EnumMap<>(UnitType.class);
    private final @NonNull AudioParameters attack_notification;
    private final @NonNull AudioParameters building_notification;
    private final @NonNull List<@NonNull MagicType> magics;
    private final @NonNull Map<@NonNull MagicType, @NonNull MagicFactory> magicFactories;
    private final @NonNull ChieftainAI chieftain_ai;
    private final @NonNull AudioParameters music;

    public RaceInfo(@NonNull Race race, @NonNull BuildingTemplate quarters, @NonNull BuildingTemplate armory,
            @NonNull BuildingTemplate tower,
            @NonNull UnitTemplate warrior_rock, @NonNull UnitTemplate warrior_iron,
            @NonNull UnitTemplate warrior_rubber,
            @NonNull UnitTemplate peon, @NonNull UnitTemplate chieftain,
            @NonNull AudioFile attack_notification, @NonNull AudioFile building_notification,
            @NonNull Map<MagicType, MagicFactory> magicFactories,
            @NonNull ChieftainAI chieftain_ai,
            @NonNull AudioParameters music) {
        this.race = race;
        buildings.put(BuildingType.QUARTERS, quarters);
        buildings.put(BuildingType.ARMORY, armory);
        buildings.put(BuildingType.TOWER, tower);
        units.put(UnitType.WARRIOR_ROCK, warrior_rock);
        units.put(UnitType.WARRIOR_IRON, warrior_iron);
        units.put(UnitType.WARRIOR_RUBBER, warrior_rubber);
        units.put(UnitType.PEON, peon);
        units.put(UnitType.CHIEFTAIN, chieftain);
        this.attack_notification = new AudioParameters(attack_notification, AudioAssets.AUDIO_RANK_NOTIFICATION,
                AudioAssets.AUDIO_DISTANCE_NOTIFICATION, AudioAssets.AUDIO_GAIN_NOTIFICATION,
                AudioAssets.AUDIO_RADIUS_NOTIFICATION,
                1f, false, true);
        this.building_notification = new AudioParameters(building_notification, AudioAssets.AUDIO_RANK_NOTIFICATION,
                AudioAssets.AUDIO_DISTANCE_NOTIFICATION, AudioAssets.AUDIO_GAIN_NOTIFICATION,
                AudioAssets.AUDIO_RADIUS_NOTIFICATION,
                1f, false, true);
        this.magics = switch (race) {
            case Race.NATIVES -> List.of(MagicType.POISON_FOG, MagicType.LIGHTNING_CLOUD);
            case Race.VIKINGS -> List.of(MagicType.STUN, MagicType.SONIC_BLAST);
        };
        this.magicFactories = new EnumMap<>(magicFactories);
        this.chieftain_ai = chieftain_ai;
        this.music = music;
    }

    public @NonNull Race getRaceType() {
        return race;
    }

    public @NonNull BuildingTemplate getBuildingTemplate(@NonNull BuildingType type) {
        BuildingTemplate template = buildings.get(type);
        if (template == null) {
            throw new IllegalArgumentException("No template registered for building type: " + type);
        }
        return template;
    }

    public @NonNull UnitTemplate getUnitTemplate(@NonNull UnitType type) {
        UnitTemplate template = units.get(type);
        if (template == null) {
            throw new IllegalArgumentException("No template registered for unit type: " + type);
        }
        return template;
    }

    public @NonNull AudioParameters getAttackNotificationAudio() {
        return attack_notification;
    }

    public @NonNull AudioParameters getBuildingNotificationAudio() {
        return building_notification;
    }

    public @NonNull List<@NonNull MagicType> getMagics() {
        return magics;
    }

    public @NonNull MagicType getMagicType(int slotIndex) {
        return magics.get(slotIndex);
    }

    public @NonNull MagicFactory getMagicFactory(@NonNull MagicType type) {
        MagicFactory factory = magicFactories.get(type);
        if (factory == null) {
            throw new IllegalArgumentException("No magic factory registered for magic type: " + type);
        }
        return factory;
    }

    public @NonNull ChieftainAI getChieftainAI() {
        return chieftain_ai;
    }

    public @NonNull AudioParameters getMusic() {
        return music;
    }
}
