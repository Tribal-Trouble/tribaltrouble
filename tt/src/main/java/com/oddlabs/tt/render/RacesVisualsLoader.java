package com.oddlabs.tt.render;

import com.oddlabs.tt.form.ProgressForm;
import com.oddlabs.tt.global.Globals;
import com.oddlabs.tt.gui.GUIIcons;
import com.oddlabs.tt.model.Abilities;
import com.oddlabs.tt.model.BuildingTemplate;
import com.oddlabs.tt.model.BuildingType;
import com.oddlabs.tt.model.EmojiType;
import com.oddlabs.tt.model.MagicType;
import com.oddlabs.tt.model.MountUnitContainerFactory;
import com.oddlabs.tt.model.Race;
import com.oddlabs.tt.model.RaceInfo;
import com.oddlabs.tt.model.RacesResources;
import com.oddlabs.tt.model.ReproduceUnitContainerFactory;
import com.oddlabs.tt.model.SupplyType;
import com.oddlabs.tt.model.UnitContainerFactory;
import com.oddlabs.tt.model.UnitSupplyContainerFactory;
import com.oddlabs.tt.model.UnitTemplate;
import com.oddlabs.tt.model.UnitType;
import com.oddlabs.tt.model.UnitVisualType;
import com.oddlabs.tt.model.WeaponVisualType;
import com.oddlabs.tt.model.WorkerUnitContainerFactory;
import com.oddlabs.tt.model.weapon.InstantHitFactory;
import com.oddlabs.tt.model.weapon.IronAxeWeapon;
import com.oddlabs.tt.model.weapon.IronSpearWeapon;
import com.oddlabs.tt.model.weapon.LightningCloudFactory;
import com.oddlabs.tt.model.weapon.MagicFactory;
import com.oddlabs.tt.model.weapon.PoisonFogFactory;
import com.oddlabs.tt.model.weapon.RockAxeWeapon;
import com.oddlabs.tt.model.weapon.RockSpearWeapon;
import com.oddlabs.tt.model.weapon.RubberAxeWeapon;
import com.oddlabs.tt.model.weapon.RubberSpearWeapon;
import com.oddlabs.tt.model.weapon.SonicBlastFactory;
import com.oddlabs.tt.model.weapon.StunFactory;
import com.oddlabs.tt.model.weapon.ThrowingFactory;
import com.oddlabs.tt.model.weapon.WeaponFactory;
import com.oddlabs.tt.player.NativeChieftainAI;
import com.oddlabs.tt.player.VikingChieftainAI;
import com.oddlabs.tt.procedural.DynamicEmojiGenerator;
import com.oddlabs.tt.procedural.GeneratorHalos;
import com.oddlabs.tt.procedural.GeneratorLightning;
import com.oddlabs.tt.procedural.GeneratorPoison;
import com.oddlabs.tt.procedural.GeneratorSmoke;
import com.oddlabs.tt.resource.AudioAssets;
import com.oddlabs.tt.resource.SpriteFile;
import com.oddlabs.tt.resource.TextureFile;
import com.oddlabs.tt.util.Utils;
import org.joml.Vector3f;
import org.jspecify.annotations.NonNull;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.util.EnumMap;
import java.util.ResourceBundle;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Loader class that centralizes client-side graphics asset registration and template loading.
 */
public final class RacesVisualsLoader {
    private static final Logger logger = Logger.getLogger(RacesVisualsLoader.class.getSimpleName());
    private static final ResourceBundle bundle = ResourceBundle.getBundle(RacesResources.class.getName());

    private static @NonNull String i18n(@NonNull String key, @NonNull Object @NonNull... args) {
        return Utils.getBundleString(bundle, key, args);
    }

    private static final int MAX_UNIT_RESOURCES = 1;

    private static @NonNull BuildingTemplate createBuildingTemplate(
            @NonNull RenderQueues queues,
            @NonNull BuildingType building_type,
            @NonNull Race race,
            @NonNull String built_name,
            float built_selection_radius,
            float built_selection_height,
            @NonNull String halfbuilt_name,
            float halfbuilt_selection_radius,
            float halfbuilt_selection_height,
            @NonNull String start_name,
            float start_selection_radius,
            float start_selection_height,
            float shadow_diameter,
            int placing_size,
            float smoke_radius,
            float smoke_height,
            int num_fragments,
            int max_hit_points,
            UnitContainerFactory unit_container_factory,
            @NonNull Abilities abilities,
            float @NonNull [] hit_offset_z,
            float mount_offset,
            float no_detail_size,
            float rally_x,
            float rally_y,
            float rally_z,
            float chimney_x,
            float chimney_y,
            float chimney_z,
            @NonNull String name) {
        assert hit_offset_z.length == 3;

        final float ring_mid = 0.445f;
        final float fadeout = 0.002f;
        final float ring_thickness = RacesResources.BUILDING_RING_PHYSICAL_THICKNESS / shadow_diameter;
        Supplier<Texture[]> building_shadow_desc = new GeneratorHalos(DecalRenderer.HALO_LUT_RESOLUTION,
                new float[][]{{0.15f, 0.5f}, {0.5f, 0f}},
                new float[][]{{ring_mid - ring_thickness / 2 - fadeout, 0f}, {ring_mid - ring_thickness / 2, 1f}, {
                        ring_mid + ring_thickness / 2, 1f}, {ring_mid + ring_thickness / 2 + fadeout, 0f}});
        ShadowListKey shadow_renderer = queues.registerSelectableShadowList(building_shadow_desc);
        SpriteFile building = new SpriteFile(built_name,
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteFile building_halfbuilt = new SpriteFile(halfbuilt_name,
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteFile building_start = new SpriteFile(start_name,
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteKey builtSprite = queues.register(building);
        SpriteKey halfbuiltSprite = queues.register(building_halfbuilt);
        SpriteKey startSprite = queues.register(building_start);

        VisualRegistry.getInstance().registerBuilding(race, building_type,
                new VisualRegistry.BuildingVisuals(startSprite, halfbuiltSprite, builtSprite, shadow_renderer));

        return new BuildingTemplate(building_type,
                placing_size,
                smoke_radius,
                smoke_height,
                num_fragments,
                shadow_diameter,
                builtSprite.bounds(),
                built_selection_radius,
                built_selection_height,
                halfbuiltSprite.bounds(),
                halfbuilt_selection_radius,
                halfbuilt_selection_height,
                startSprite.bounds(),
                start_selection_radius,
                start_selection_height,
                max_hit_points,
                unit_container_factory,
                abilities,
                hit_offset_z,
                mount_offset,
                no_detail_size,
                0f,
                new Vector3f(rally_x, rally_y, rally_z),
                new Vector3f(chimney_x, chimney_y, chimney_z),
                name);
    }

    public static @NonNull RacesResources load(@NonNull RenderQueues queues) {
        int num_progress = 23;
        SpriteFile native_rock_sprite = new SpriteFile("/geometry/natives/rock_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile native_wood_sprite = new SpriteFile("/geometry/natives/wood_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteFile native_rubber_sprite = new SpriteFile("/geometry/natives/rubber_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        EnumMap<SupplyType, SpriteKey> nativeMap = new EnumMap<>(SupplyType.class);
        nativeMap.put(SupplyType.WOOD, queues.register(native_wood_sprite));
        nativeMap.put(SupplyType.ROCK, queues.register(native_rock_sprite));
        nativeMap.put(SupplyType.IRON, queues.register(native_rock_sprite, 1));
        nativeMap.put(SupplyType.RUBBER, queues.register(native_rubber_sprite));
        for (var entry : nativeMap.entrySet()) {
            VisualRegistry.getInstance().registerCarriedSupply(Race.NATIVES, entry.getKey(), entry.getValue());
        }

        SpriteFile viking_wood_sprite = new SpriteFile("/geometry/vikings/wood_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteFile viking_rubber_sprite = new SpriteFile("/geometry/vikings/rubber_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile viking_rock_sprite = new SpriteFile("/geometry/vikings/rock_resource.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        EnumMap<SupplyType, SpriteKey> vikingMap = new EnumMap<>(SupplyType.class);
        vikingMap.put(SupplyType.WOOD, queues.register(viking_wood_sprite));
        vikingMap.put(SupplyType.ROCK, queues.register(viking_rock_sprite));
        vikingMap.put(SupplyType.IRON, queues.register(viking_rock_sprite, 1));
        vikingMap.put(SupplyType.RUBBER, queues.register(viking_rubber_sprite));
        for (var entry : vikingMap.entrySet()) {
            VisualRegistry.getInstance().registerCarriedSupply(Race.VIKINGS, entry.getKey(), entry.getValue());
        }

        TextureKey[] smoke_textures = new TextureKey[1];
        smoke_textures[0] = queues.registerEffectTexture(new GeneratorSmoke(42, 0.6f, 1.0f), 0, 0);
        VisualRegistry.getInstance().registerSmokeTextures(smoke_textures);

        TextureKey[] damage_smoke_textures = new TextureKey[1];
        damage_smoke_textures[0] = queues.registerEffectTexture(new GeneratorSmoke(43, 1.0f, 0.5f), 0, 1);
        VisualRegistry.getInstance().registerDamageSmokeTextures(damage_smoke_textures);

        TextureKey[] poison_textures = new TextureKey[1];
        poison_textures[0] = queues.registerEffectTexture(new GeneratorPoison(), 0, 2);
        VisualRegistry.getInstance().registerPoisonTextures(poison_textures);

        TextureKey lightning_texture = queues.registerEffectTexture(new GeneratorLightning(), 0, 3);
        VisualRegistry.getInstance().registerLightningTexture(lightning_texture);

        TextureKey[] note_textures = new TextureKey[8];
        for (int i = 0; i < note_textures.length; i++) {
            note_textures[i] = queues.registerEffectTexture(new TextureFile("/textures/effects/note" + (i + 1),
                    Globals.COMPRESSED_RGBA_FORMAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR,
                    GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE), 4 + i);
        }
        VisualRegistry.getInstance().registerNoteTextures(note_textures);

        TextureKey[] star_textures = new TextureKey[1];
        star_textures[0] = queues.registerEffectTexture(new TextureFile("/textures/effects/star",
                Globals.COMPRESSED_RGBA_FORMAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR,
                GL12.GL_CLAMP_TO_EDGE, GL12.GL_CLAMP_TO_EDGE), 12);
        VisualRegistry.getInstance().registerStarTextures(star_textures);

        queues.ensureTextureArray();

        ProgressForm.progress(1f / num_progress);

        BuildingTemplate viking_quarters_template = createBuildingTemplate(
                queues,
                BuildingType.QUARTERS,
                Race.VIKINGS,
                "/geometry/vikings/quarters.binsprite",
                3.5f, 7f,
                "/geometry/vikings/quarters_halfbuilt.binsprite",
                3.5f, 6f,
                "/geometry/vikings/quarters_start.binsprite",
                5f, 1f,
                22f, RacesResources.QUARTERS_SIZE, 6f, 9f, 30, RacesResources.QUARTERS_HIT_POINTS,
                new ReproduceUnitContainerFactory(),
                new Abilities(Abilities.REPRODUCE | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                3.65f, .25f, 8f,
                0f, 0f, 0f,
                i18n("quarters"));
        ProgressForm.progress(1f / num_progress);
        BuildingTemplate viking_armory_template = createBuildingTemplate(
                queues,
                BuildingType.ARMORY,
                Race.VIKINGS,
                "/geometry/vikings/armory.binsprite",
                3.5f, 7f,
                "/geometry/vikings/armory_halfbuilt.binsprite",
                3.5f, 6f,
                "/geometry/vikings/armory_start.binsprite",
                5f, 1f,
                22f, RacesResources.ARMORY_SIZE, 6f, 9f, 30, RacesResources.ARMORY_HIT_POINTS,
                new WorkerUnitContainerFactory(),
                new Abilities(Abilities.SUPPLY_CONTAINER | Abilities.BUILD_ARMIES | Abilities.RALLY_TO
                        | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                0f, 2.25f, 10f,
                .25f, -2.8f, 13.1f,
                i18n("armory"));
        ProgressForm.progress(1f / num_progress);
        BuildingTemplate viking_tower_template = createBuildingTemplate(
                queues,
                BuildingType.TOWER,
                Race.VIKINGS,
                "/geometry/vikings/tower.binsprite",
                1.25f, 11f,
                "/geometry/vikings/tower_halfbuilt.binsprite",
                2f, 7f,
                "/geometry/vikings/tower_start.binsprite",
                2.5f, 1f,
                10f, RacesResources.TOWER_SIZE, 3f, 12f, 20, RacesResources.TOWER_HIT_POINTS,
                new MountUnitContainerFactory(),
                new Abilities(Abilities.ATTACK | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 2f, 7.5f}, 9.55f, 2.5f,
                .85f, .85f, 9.5f,
                0f, 0f, 0f,
                i18n("tower"));
        ProgressForm.progress(1f / num_progress);
        BuildingTemplate native_quarters_template = createBuildingTemplate(
                queues,
                BuildingType.QUARTERS,
                Race.NATIVES,
                "/geometry/natives/quarters.binsprite",
                4f, 8f,
                "/geometry/natives/quarters_halfbuilt.binsprite",
                4f, 6f,
                "/geometry/natives/quarters_start.binsprite",
                5f, 1f,
                16f, RacesResources.QUARTERS_SIZE, 6f, 9f, 30, RacesResources.QUARTERS_HIT_POINTS,
                new ReproduceUnitContainerFactory(),
                new Abilities(Abilities.REPRODUCE | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                -1.15f, -.77f, 11f,
                0f, 0f, 0f,
                i18n("quarters"));
        ProgressForm.progress(1f / num_progress);
        BuildingTemplate native_armory_template = createBuildingTemplate(
                queues,
                BuildingType.ARMORY,
                Race.NATIVES,
                "/geometry/natives/armory.binsprite",
                4f, 8f,
                "/geometry/natives/armory_halfbuilt.binsprite",
                4f, 6f,
                "/geometry/natives/armory_start.binsprite",
                5f, 1f,
                16f, RacesResources.ARMORY_SIZE, 6f, 9f, 30, RacesResources.ARMORY_HIT_POINTS,
                new WorkerUnitContainerFactory(),
                new Abilities(Abilities.SUPPLY_CONTAINER | Abilities.BUILD_ARMIES | Abilities.RALLY_TO
                        | Abilities.TARGET),
                new float[]{0f, 1f, 3f}, 0f, 6f,
                0f, -.4f, 12f,
                0f, -1f, 11.5f,
                i18n("armory"));
        ProgressForm.progress(1f / num_progress);
        BuildingTemplate native_tower_template = createBuildingTemplate(
                queues,
                BuildingType.TOWER,
                Race.NATIVES,
                "/geometry/natives/tower.binsprite",
                1f, 14f,
                "/geometry/natives/tower_halfbuilt.binsprite",
                1f, 14f,
                "/geometry/natives/tower_start.binsprite",
                1.5f, 2f,
                5f, RacesResources.TOWER_SIZE, 3f, 12f, 20, RacesResources.TOWER_HIT_POINTS,
                new MountUnitContainerFactory(),
                new Abilities(Abilities.ATTACK | Abilities.RALLY_TO | Abilities.TARGET),
                new float[]{0f, 11.5f, 11.5f}, 13f, 2.5f,
                .95f, 0f, 13f,
                0f, 0f, 0f,
                i18n("tower"));
        ProgressForm.progress(1f / num_progress);
        final float shadow_diameter_warrior = 1.9f;
        final float shadow_diameter_peon = 1.6f;
        final float shadow_diameter_chieftain = 2.2f;
        ProgressForm.progress(1f / num_progress);

        SpriteFile sprite_list_warrior = new SpriteFile("/geometry/vikings/warrior.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);

        SpriteFile sprite_list_chieftain = new SpriteFile("/geometry/vikings/chieftain.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile sprite_list_native_chieftain = new SpriteFile("/geometry/natives/chieftain.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        SpriteFile sprite_list_peon = new SpriteFile("/geometry/vikings/peon.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile sprite_list_native_peon = new SpriteFile("/geometry/natives/peon.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile sprite_list_native_warrior = new SpriteFile("/geometry/natives/warrior.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile viking_warrior_axe = new SpriteFile("/geometry/vikings/axe.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);
        SpriteFile native_warrior_spear = new SpriteFile("/geometry/natives/spear.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false);
        ProgressForm.progress(1f / num_progress);

        WeaponFactory viking_warrior_rock_weapon = new ThrowingFactory<>(RockAxeWeapon.class, RockAxeWeapon::new, 0.5f,
                RacesResources.THROW_RANGE, 29f / 58f, AudioAssets.SFX_WEAPON_AXE, AudioAssets.SFX_IMPACT_MEATS);
        VisualRegistry.getInstance().registerWeapon(Race.VIKINGS, WeaponVisualType.ROCK,
                queues.register(viking_warrior_axe, UnitType.WARRIOR_ROCK.getValue()));

        WeaponFactory viking_warrior_iron_weapon = new ThrowingFactory<>(IronAxeWeapon.class, IronAxeWeapon::new, 0.75f,
                RacesResources.THROW_RANGE, 29f / 58f, AudioAssets.SFX_WEAPON_AXE, AudioAssets.SFX_IMPACT_MEATS);
        VisualRegistry.getInstance().registerWeapon(Race.VIKINGS, WeaponVisualType.IRON,
                queues.register(viking_warrior_axe, UnitType.WARRIOR_IRON.getValue()));

        WeaponFactory viking_warrior_rubber_weapon = new ThrowingFactory<>(RubberAxeWeapon.class, RubberAxeWeapon::new,
                0.95f, RacesResources.THROW_RANGE, 29f / 58f, AudioAssets.SFX_WEAPON_AXE, AudioAssets.SFX_IMPACT_MEATS);
        VisualRegistry.getInstance().registerWeapon(Race.VIKINGS, WeaponVisualType.RUBBER,
                queues.register(viking_warrior_axe, UnitType.WARRIOR_RUBBER.getValue()));

        WeaponFactory native_warrior_rock_weapon = new ThrowingFactory<>(RockSpearWeapon.class, RockSpearWeapon::new,
                0.5f, RacesResources.THROW_RANGE, 46f / 100f, AudioAssets.SFX_WEAPON_SPEAR,
                AudioAssets.SFX_IMPACT_MEATS);
        VisualRegistry.getInstance().registerWeapon(Race.NATIVES, WeaponVisualType.ROCK,
                queues.register(native_warrior_spear, UnitType.WARRIOR_ROCK.getValue()));

        WeaponFactory native_warrior_iron_weapon = new ThrowingFactory<>(IronSpearWeapon.class, IronSpearWeapon::new,
                0.75f, RacesResources.THROW_RANGE, 46f / 100f, AudioAssets.SFX_WEAPON_SPEAR,
                AudioAssets.SFX_IMPACT_MEATS);
        VisualRegistry.getInstance().registerWeapon(Race.NATIVES, WeaponVisualType.IRON,
                queues.register(native_warrior_spear, UnitType.WARRIOR_IRON.getValue()));

        WeaponFactory native_warrior_rubber_weapon = new ThrowingFactory<>(RubberSpearWeapon.class,
                RubberSpearWeapon::new,
                0.95f, RacesResources.THROW_RANGE, 46f / 100f, AudioAssets.SFX_WEAPON_SPEAR,
                AudioAssets.SFX_IMPACT_MEATS);
        VisualRegistry.getInstance().registerWeapon(Race.NATIVES, WeaponVisualType.RUBBER,
                queues.register(native_warrior_spear, UnitType.WARRIOR_RUBBER.getValue()));

        ProgressForm.progress(1f / num_progress);
        ShadowListKey default_shadow_list = queues.registerSelectableShadowList(VisualRegistry.DEFAULT_SHADOW_DESC);
        VisualRegistry.getInstance().registerDefaultUnitShadow(default_shadow_list);
        SpriteKey vRockSprite = queues.register(sprite_list_warrior, UnitType.WARRIOR_ROCK.getValue());
        UnitTemplate viking_warrior_rock_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f,
                viking_warrior_rock_weapon,
                UnitVisualType.WARRIOR_ROCK,
                vRockSprite.bounds(),
                vRockSprite.animTypes(),
                shadow_diameter_warrior,
                null,
                AudioAssets.SFX_DEATH_VIKING_WARRIORS[0],
                .25f,
                new float[]{1.2f},
                1f,
                .5f,
                i18n("rock_warrior"),
                1,
                0f, 0f, 2f,
                3);
        VisualRegistry.getInstance().registerUnit(Race.VIKINGS, UnitVisualType.WARRIOR_ROCK, vRockSprite);

        SpriteKey vIronSprite = queues.register(sprite_list_warrior, UnitType.WARRIOR_IRON.getValue());
        UnitTemplate viking_warrior_iron_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f,
                viking_warrior_iron_weapon,
                UnitVisualType.WARRIOR_IRON,
                vIronSprite.bounds(),
                vIronSprite.animTypes(),
                shadow_diameter_warrior,
                null,
                AudioAssets.SFX_DEATH_VIKING_WARRIORS[1],
                .25f,
                new float[]{1.2f},
                1f,
                .7f,
                i18n("iron_warrior"),
                1,
                0f, 0f, 2f,
                5);
        VisualRegistry.getInstance().registerUnit(Race.VIKINGS, UnitVisualType.WARRIOR_IRON, vIronSprite);

        SpriteKey vRubberSprite = queues.register(sprite_list_warrior, UnitType.WARRIOR_RUBBER.getValue());
        UnitTemplate viking_warrior_rubber_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f,
                viking_warrior_rubber_weapon,
                UnitVisualType.WARRIOR_RUBBER,
                vRubberSprite.bounds(),
                vRubberSprite.animTypes(),
                shadow_diameter_warrior,
                null,
                AudioAssets.SFX_DEATH_VIKING_WARRIORS[1],
                .25f,
                new float[]{1.2f},
                1f,
                .7f,
                i18n("chicken_warrior"),
                1,
                0f, 0f, 2f,
                10);
        VisualRegistry.getInstance().registerUnit(Race.VIKINGS, UnitVisualType.WARRIOR_RUBBER, vRubberSprite);

        SpriteKey nRockSprite = queues.register(sprite_list_native_warrior, UnitType.WARRIOR_ROCK.getValue());
        UnitTemplate native_warrior_rock_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f,
                native_warrior_rock_weapon,
                UnitVisualType.WARRIOR_ROCK,
                nRockSprite.bounds(),
                nRockSprite.animTypes(),
                shadow_diameter_warrior,
                null,
                AudioAssets.SFX_DEATH_NATIVE_WARRIORS[0],
                .25f,
                new float[]{1.2f},
                1f,
                .5f,
                i18n("rock_warrior"),
                1,
                0f, 0f, 2f,
                3);
        VisualRegistry.getInstance().registerUnit(Race.NATIVES, UnitVisualType.WARRIOR_ROCK, nRockSprite);

        SpriteKey nIronSprite = queues.register(sprite_list_native_warrior, UnitType.WARRIOR_IRON.getValue());
        UnitTemplate native_warrior_iron_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f,
                native_warrior_iron_weapon,
                UnitVisualType.WARRIOR_IRON,
                nIronSprite.bounds(),
                nIronSprite.animTypes(),
                shadow_diameter_warrior,
                null,
                AudioAssets.SFX_DEATH_NATIVE_WARRIORS[1],
                .25f,
                new float[]{1.2f},
                1f,
                .7f,
                i18n("iron_warrior"),
                1,
                0f, 0f, 2f,
                5);
        VisualRegistry.getInstance().registerUnit(Race.NATIVES, UnitVisualType.WARRIOR_IRON, nIronSprite);

        SpriteKey nRubberSprite = queues.register(sprite_list_native_warrior, UnitType.WARRIOR_RUBBER.getValue());
        UnitTemplate native_warrior_rubber_template = new UnitTemplate(.4f,
                1.2f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.THROW),
                4f,
                native_warrior_rubber_weapon,
                UnitVisualType.WARRIOR_RUBBER,
                nRubberSprite.bounds(),
                nRubberSprite.animTypes(),
                shadow_diameter_warrior,
                null,
                AudioAssets.SFX_DEATH_NATIVE_WARRIORS[1],
                .25f,
                new float[]{1.2f},
                1f,
                .7f,
                i18n("chicken_warrior"),
                1,
                0f, 0f, 2f,
                10);
        VisualRegistry.getInstance().registerUnit(Race.NATIVES, UnitVisualType.WARRIOR_RUBBER, nRubberSprite);

        SpriteKey vPeonSprite = queues.register(sprite_list_peon);
        UnitTemplate viking_peon_template = new UnitTemplate(.4f,
                1.1f,
                new Abilities(Abilities.BUILD | Abilities.HARVEST | Abilities.ATTACK | Abilities.TARGET),
                5f,
                new InstantHitFactory(1 / 5f, 0f, 11f / 38f, AudioAssets.SFX_IMPACT_MEATS),
                UnitVisualType.PEON,
                vPeonSprite.bounds(),
                vPeonSprite.animTypes(),
                shadow_diameter_peon,
                new UnitSupplyContainerFactory(MAX_UNIT_RESOURCES),
                AudioAssets.SFX_DEATH_PEON,
                .25f,
                new float[]{.7f},
                1f,
                0f,
                i18n("peon"),
                1,
                .1f, 0f, 1.75f,
                1);
        VisualRegistry.getInstance().registerUnit(Race.VIKINGS, UnitVisualType.PEON, vPeonSprite);

        SpriteKey nPeonSprite = queues.register(sprite_list_native_peon);
        UnitTemplate native_peon_template = new UnitTemplate(.4f,
                1.1f,
                new Abilities(Abilities.BUILD | Abilities.HARVEST | Abilities.ATTACK | Abilities.TARGET),
                5f,
                new InstantHitFactory(1 / 5f, 0f, 51f / 83f, AudioAssets.SFX_IMPACT_MEATS),
                UnitVisualType.PEON,
                nPeonSprite.bounds(),
                nPeonSprite.animTypes(),
                shadow_diameter_peon,
                new UnitSupplyContainerFactory(MAX_UNIT_RESOURCES),
                AudioAssets.SFX_DEATH_PEON,
                .25f,
                new float[]{.7f},
                1f,
                0f,
                i18n("peon"),
                1,
                0f, 0f, 1.75f,
                1);
        VisualRegistry.getInstance().registerUnit(Race.NATIVES, UnitVisualType.PEON, nPeonSprite);

        SpriteKey vChieftainSprite = queues.register(sprite_list_chieftain);
        UnitTemplate viking_chieftain_template = new UnitTemplate(.4f,
                1.4f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.MAGIC),
                4f,
                new InstantHitFactory(3 / 4f, 0f, 75f / 119f, AudioAssets.SFX_VIKING_CHIEFTAIN_HITS),
                UnitVisualType.CHIEFTAIN,
                vChieftainSprite.bounds(),
                vChieftainSprite.animTypes(),
                shadow_diameter_chieftain,
                null,
                AudioAssets.SFX_DEATH_VIKING_WARRIORS[1],
                .15f,
                new float[]{1.7f},
                1f,
                0.5f,
                i18n("chieftain"),
                RacesResources.VIKING_CHIEFTAIN_HIT_POINTS,
                -.07f, .312f, 2.7f,
                40);
        VisualRegistry.getInstance().registerUnit(Race.VIKINGS, UnitVisualType.CHIEFTAIN, vChieftainSprite);

        SpriteKey nChieftainSprite = queues.register(sprite_list_native_chieftain);
        UnitTemplate native_chieftain_template = new UnitTemplate(.4f,
                1.4f,
                new Abilities(Abilities.ATTACK | Abilities.TARGET | Abilities.MAGIC),
                4f,
                new InstantHitFactory(3 / 4f, 0f, 75f / 129f, AudioAssets.SFX_NATIVE_CHIEFTAIN_HITS),
                UnitVisualType.CHIEFTAIN,
                nChieftainSprite.bounds(),
                nChieftainSprite.animTypes(),
                shadow_diameter_chieftain,
                null,
                AudioAssets.SFX_DEATH_NATIVE_WARRIORS[1],
                .15f,
                new float[]{1.7f},
                1f,
                0.5f,
                i18n("chieftain"),
                RacesResources.NATIVE_CHIEFTAIN_HIT_POINTS,
                .878f, .151f, 2.8f,
                40);
        VisualRegistry.getInstance().registerUnit(Race.NATIVES, UnitVisualType.CHIEFTAIN, nChieftainSprite);

        EnumMap<MagicType, MagicFactory> native_magic = new EnumMap<>(MagicType.class);
        native_magic.put(MagicType.POISON_FOG, new PoisonFogFactory(0.9f, 0f, 0.55f, 26f, .5f, 2f, 20f, 10, 5f, 80f
                / 224f,
                163f / 224f));
        native_magic.put(MagicType.LIGHTNING_CLOUD, new LightningCloudFactory(0.9f, 0f, 0.55f, 22f, 1f, 8f, 1f, 30, 18f,
                5f,
                80f / 224f, 163f / 224f));

        EnumMap<MagicType, MagicFactory> viking_magic = new EnumMap<>(MagicType.class);
        viking_magic.put(MagicType.STUN, new StunFactory(2.57f, 0f, 3.8f, 36f, 30f, 10f, 6f, 57f / 159f, 100f / 159f));
        viking_magic.put(MagicType.SONIC_BLAST, new SonicBlastFactory(2.57f, 0f, 3.8f, 36f, 17f, 2f, 150, 30, .8f, 6f,
                57f
                        / 159f, 100f / 159f));

        ProgressForm.progress(1f / num_progress);
        GUIIcons icons = GUIIcons.getIcons();
        SpriteKey nativeRallyPoint = queues.register(new SpriteFile("/geometry/natives/rally_point.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        VisualRegistry.getInstance().registerRallyPoint(Race.NATIVES, nativeRallyPoint);

        RaceInfo natives_raceInfo = new RaceInfo(
                Race.NATIVES,
                native_quarters_template,
                native_armory_template,
                native_tower_template,
                native_warrior_rock_template,
                native_warrior_iron_template,
                native_warrior_rubber_template,
                native_peon_template,
                native_chieftain_template,
                AudioAssets.SFX_ATTACKNOTIFY_NATIVE,
                AudioAssets.SFX_BUILDINGNOTIFY_NATIVE,
                native_magic,
                new NativeChieftainAI(),
                AudioAssets.MUSIC_NATIVE);

        SpriteKey vikingRallyPoint = queues.register(new SpriteFile("/geometry/vikings/rally_point.binsprite",
                Globals.NO_MIPMAP_CUTOFF, true, true, true, false));
        VisualRegistry.getInstance().registerRallyPoint(Race.VIKINGS, vikingRallyPoint);

        RaceInfo vikings_raceInfo = new RaceInfo(
                Race.VIKINGS,
                viking_quarters_template,
                viking_armory_template,
                viking_tower_template,
                viking_warrior_rock_template,
                viking_warrior_iron_template,
                viking_warrior_rubber_template,
                viking_peon_template,
                viking_chieftain_template,
                AudioAssets.SFX_ATTACKNOTIFY_VIKING,
                AudioAssets.SFX_BUILDINGNOTIFY_VIKING,
                viking_magic,
                new VikingChieftainAI(),
                AudioAssets.MUSIC_VIKING);

        EnumMap<Race, @NonNull RaceInfo> raceInfos = new EnumMap<>(Race.class);
        raceInfos.put(Race.NATIVES, natives_raceInfo);
        raceInfos.put(Race.VIKINGS, vikings_raceInfo);

        SpriteKey[] wood_fragment_sprites = new SpriteKey[4];
        wood_fragment_sprites[0] = queues.register(new SpriteFile("/geometry/misc/wood_2.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false), 0);
        wood_fragment_sprites[1] = queues.register(new SpriteFile("/geometry/misc/wood_3.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        wood_fragment_sprites[2] = queues.register(new SpriteFile("/geometry/misc/wood_4.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        wood_fragment_sprites[3] = queues.register(new SpriteFile("/geometry/misc/wood_5.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        VisualRegistry.getInstance().registerWoodFragments(wood_fragment_sprites);

        SpriteKey[] treasure_sprites = new SpriteKey[6];
        treasure_sprites[0] = queues.register(new SpriteFile("/geometry/misc/icon.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        treasure_sprites[1] = queues.register(new SpriteFile("/geometry/misc/treasure_1.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        treasure_sprites[2] = queues.register(new SpriteFile("/geometry/misc/treasure_2.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        treasure_sprites[3] = queues.register(new SpriteFile("/geometry/misc/treasure_3.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        treasure_sprites[4] = queues.register(new SpriteFile("/geometry/misc/treasure_4.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        treasure_sprites[5] = queues.register(new SpriteFile("/geometry/misc/treasure_5.binsprite",
                Globals.NO_MIPMAP_CUTOFF,
                true, true, true, false));
        VisualRegistry.getInstance().registerTreasures(treasure_sprites);

        logger.info("RacesResources: beginning emoji sprite registration");
        SpriteKey gravestone_emoji_sprite = queues.registerDynamicSprite(
                SpriteList.getQuadInstance(),
                queues.registerTexture(new DynamicEmojiGenerator("🪦"), 0)
        );
        SpriteKey[] chicken_emoji_sprites = "🐓🥚🐣🌽🐛".codePoints()
                .mapToObj(DynamicEmojiGenerator::new)
                .map(tg -> queues.registerTexture(tg, 0))
                .map(tk -> queues.registerDynamicSprite(SpriteList.getQuadInstance(), tk))
                .toArray(SpriteKey[]::new);
        SpriteKey saw_emoji_sprite = queues.registerDynamicSprite(
                SpriteList.getQuadInstance(),
                queues.registerTexture(new DynamicEmojiGenerator("🪚"), 0)
        );
        SpriteKey hammer_emoji_sprite = queues.registerDynamicSprite(
                SpriteList.getQuadInstance(),
                queues.registerTexture(new DynamicEmojiGenerator("🔨"), 0)
        );

        SpriteKey tree_status_sprite = queues.registerIconSprite(icons.getTreeStatusIcon());
        SpriteKey rock_status_sprite = queues.registerIconSprite(icons.getRockStatusIcon());
        SpriteKey iron_status_sprite = queues.registerIconSprite(icons.getIronStatusIcon());
        SpriteKey rubber_status_sprite = queues.registerIconSprite(icons.getRubberStatusIcon());

        // Register visual emojis and status sprites in registry
        VisualRegistry vr = VisualRegistry.getInstance();
        vr.registerEmoji(EmojiType.GRAVESTONE, gravestone_emoji_sprite);
        vr.registerEmoji(EmojiType.REPAIR_SAW, saw_emoji_sprite);
        vr.registerEmoji(EmojiType.REPAIR_HAMMER, hammer_emoji_sprite);
        vr.registerChickenCluckSprites(chicken_emoji_sprites);
        vr.registerEmoji(EmojiType.HARVEST_WOOD, tree_status_sprite);
        vr.registerEmoji(EmojiType.HARVEST_ROCK, rock_status_sprite);
        vr.registerEmoji(EmojiType.HARVEST_IRON, iron_status_sprite);
        vr.registerEmoji(EmojiType.HARVEST_RUBBER, rubber_status_sprite);

        ProgressForm.progress(1f / num_progress);
        ProgressForm.progress(1f / num_progress);

        return new RacesResources(raceInfos);
    }
}
