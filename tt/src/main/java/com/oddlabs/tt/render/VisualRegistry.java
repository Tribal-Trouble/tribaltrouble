package com.oddlabs.tt.render;

import com.oddlabs.tt.model.BuildingType;
import com.oddlabs.tt.model.EmojiType;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.SupplyType;
import com.oddlabs.tt.model.UnitVisualType;
import com.oddlabs.tt.model.WeaponVisualType;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.oddlabs.tt.procedural.GeneratorCrack;
import com.oddlabs.tt.procedural.GeneratorHalos;
import java.util.EnumMap;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Client-side registry mapping simulation visual types to graphics-bound SpriteKeys and ShadowListKeys.
 */
public final class VisualRegistry {
    public static final GeneratorHalos DEFAULT_SHADOW_DESC = new GeneratorHalos(DecalRenderer.HALO_LUT_RESOLUTION,
            new float[][]{{0f, 0.75f}, {0.5f, 0f}}, new float[][]{{0.40f, 0f}, {0.41f, 1f}, {0.48f, 1f}, {0.49f, 0f}});

    public static final GeneratorCrack CRACK_DECAL_DESC = new GeneratorCrack();

    private static final VisualRegistry INSTANCE = new VisualRegistry();

    public static @NonNull VisualRegistry getInstance() {
        return INSTANCE;
    }

    public record BuildingVisuals(
                                  @NonNull SpriteKey start,
                                  @NonNull SpriteKey halfbuilt,
                                  @NonNull SpriteKey built,
                                  @NonNull ShadowListKey shadow
    ) {
    }

    private final EnumMap<Race, EnumMap<UnitVisualType, SpriteKey>> units = new EnumMap<>(Race.class);
    private final EnumMap<Race, EnumMap<BuildingType, BuildingVisuals>> buildings = new EnumMap<>(Race.class);
    private final EnumMap<Race, EnumMap<WeaponVisualType, SpriteKey>> weapons = new EnumMap<>(Race.class);
    private final EnumMap<Race, EnumMap<SupplyType, SpriteKey>> carriedSupplies = new EnumMap<>(Race.class);
    private final EnumMap<EmojiType, SpriteKey> emojis = new EnumMap<>(EmojiType.class);
    private final EnumMap<Race, SpriteKey> rallyPoints = new EnumMap<>(Race.class);
    private @NonNull SpriteKey @Nullable [] chickenCluckSprites;
    private @Nullable ShadowListKey defaultUnitShadow;
    private @NonNull TextureKey @Nullable [] smokeTextures;
    private @NonNull TextureKey @Nullable [] damageSmokeTextures;
    private @NonNull TextureKey @Nullable [] poisonTextures;
    private @Nullable TextureKey lightningTexture;
    private @NonNull TextureKey @Nullable [] noteTextures;
    private @NonNull TextureKey @Nullable [] starTextures;
    private @NonNull SpriteKey @Nullable [] woodFragments;
    private @NonNull SpriteKey @Nullable [] treasures;

    private VisualRegistry() {
        for (Race race : Race.values()) {
            units.put(race, new EnumMap<>(UnitVisualType.class));
            buildings.put(race, new EnumMap<>(BuildingType.class));
            weapons.put(race, new EnumMap<>(WeaponVisualType.class));
            carriedSupplies.put(race, new EnumMap<>(SupplyType.class));
        }
    }

    public void registerChickenCluckSprites(SpriteKey @NonNull [] sprites) {
        this.chickenCluckSprites = sprites.clone();
    }

    public void registerWeapon(@NonNull Race race, @NonNull WeaponVisualType type, @NonNull SpriteKey sprite) {
        weapons.get(race).put(type, sprite);
    }

    public @NonNull SpriteKey getWeaponSprite(@NonNull Race race, @NonNull WeaponVisualType type) {
        SpriteKey sprite = weapons.get(race).get(type);
        if (sprite == null) {
            throw new IllegalStateException("Weapon sprite not registered for race " + race + " and type " + type);
        }
        return sprite;
    }

    public void registerRallyPoint(@NonNull Race race, @NonNull SpriteKey sprite) {
        rallyPoints.put(race, sprite);
    }

    public @NonNull SpriteKey getRallyPoint(@NonNull Race race) {
        SpriteKey sprite = rallyPoints.get(race);
        if (sprite == null) {
            throw new IllegalStateException("Rally point sprite not registered for race " + race);
        }
        return sprite;
    }

    public void registerUnit(@NonNull Race race, @NonNull UnitVisualType type, @NonNull SpriteKey sprite) {
        units.get(race).put(type, sprite);
    }

    public @NonNull SpriteKey getUnitSprite(@NonNull Race race, @NonNull UnitVisualType type) {
        SpriteKey sprite = units.get(race).get(type);
        if (sprite == null) {
            throw new IllegalStateException("Unit sprite not registered for race " + race + " and type " + type);
        }
        return sprite;
    }

    public void registerBuilding(@NonNull Race race, @NonNull BuildingType type,
            @NonNull BuildingVisuals visuals) {
        buildings.get(race).put(type, visuals);
    }

    public @NonNull BuildingVisuals getBuildingVisuals(@NonNull Race race, @NonNull BuildingType type) {
        BuildingVisuals visuals = buildings.get(race).get(type);
        if (visuals == null) {
            throw new IllegalStateException("Building visuals not registered for race " + race + " and type " + type);
        }
        return visuals;
    }

    public void registerEmoji(@NonNull EmojiType type, @NonNull SpriteKey sprite) {
        emojis.put(type, sprite);
    }

    public @NonNull Optional<SpriteKey> getEmojiSprite(@NonNull EmojiType type) {
        if (type == EmojiType.CHICKEN_CLUCK && chickenCluckSprites != null && chickenCluckSprites.length > 0) {
            return Optional.of(chickenCluckSprites[ThreadLocalRandom.current().nextInt(
                    chickenCluckSprites.length)]);
        }

        return Optional.ofNullable(emojis.get(type));
    }

    public void registerDefaultUnitShadow(@NonNull ShadowListKey shadow) {
        this.defaultUnitShadow = shadow;
    }

    public @NonNull ShadowListKey getDefaultUnitShadow() {
        if (defaultUnitShadow == null) {
            throw new IllegalStateException("Default unit shadow not registered");
        }
        return defaultUnitShadow;
    }

    public void registerCarriedSupply(@NonNull Race race, @NonNull SupplyType type, @NonNull SpriteKey sprite) {
        carriedSupplies.get(race).put(type, sprite);
    }

    public @NonNull SpriteKey getCarriedSupplySprite(@NonNull Race race, @NonNull SupplyType type) {
        SpriteKey sprite = carriedSupplies.get(race).get(type);
        if (sprite == null) {
            throw new IllegalStateException("Carried supply sprite not registered for race " + race + " and type "
                    + type);
        }
        return sprite;
    }

    public void registerSmokeTextures(TextureKey @NonNull [] textures) {
        this.smokeTextures = textures.clone();
    }

    public @NonNull TextureKey @NonNull [] getSmokeTextures() {
        if (smokeTextures == null) {
            throw new IllegalStateException("Smoke textures not registered");
        }
        return smokeTextures;
    }

    public void registerDamageSmokeTextures(TextureKey @NonNull [] textures) {
        this.damageSmokeTextures = textures.clone();
    }

    public @NonNull TextureKey @NonNull [] getDamageSmokeTextures() {
        if (damageSmokeTextures == null) {
            throw new IllegalStateException("Damage smoke textures not registered");
        }
        return damageSmokeTextures;
    }

    public void registerPoisonTextures(TextureKey @NonNull [] textures) {
        this.poisonTextures = textures.clone();
    }

    public @NonNull TextureKey @NonNull [] getPoisonTextures() {
        if (poisonTextures == null) {
            throw new IllegalStateException("Poison textures not registered");
        }
        return poisonTextures;
    }

    public void registerLightningTexture(@NonNull TextureKey texture) {
        this.lightningTexture = texture;
    }

    public @NonNull TextureKey getLightningTexture() {
        if (lightningTexture == null) {
            throw new IllegalStateException("Lightning texture not registered");
        }
        return lightningTexture;
    }

    public void registerNoteTextures(TextureKey @NonNull [] textures) {
        this.noteTextures = textures.clone();
    }

    public @NonNull TextureKey @NonNull [] getNoteTextures() {
        if (noteTextures == null) {
            throw new IllegalStateException("Note textures not registered");
        }
        return noteTextures;
    }

    public void registerStarTextures(TextureKey @NonNull [] textures) {
        this.starTextures = textures.clone();
    }

    public @NonNull TextureKey @NonNull [] getStarTextures() {
        if (starTextures == null) {
            throw new IllegalStateException("Star textures not registered");
        }
        return starTextures;
    }

    public void registerWoodFragments(SpriteKey @NonNull [] sprites) {
        this.woodFragments = sprites.clone();
    }

    public @NonNull SpriteKey @NonNull [] getWoodFragments() {
        if (woodFragments == null) {
            throw new IllegalStateException("Wood fragments not registered");
        }
        return woodFragments;
    }

    public void registerTreasures(SpriteKey @NonNull [] sprites) {
        this.treasures = sprites.clone();
    }

    public @NonNull SpriteKey @NonNull [] getTreasures() {
        if (treasures == null) {
            throw new IllegalStateException("Treasures not registered");
        }
        return treasures;
    }
}
