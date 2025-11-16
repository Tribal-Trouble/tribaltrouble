package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

public final class Abilities {
    // No abilities
    public static final int NONE = 0;
    // Can build/repair buildings
    public static final int BUILD = 1;
    // Can attack other units
    public static final int ATTACK = 2;
    // Can harvest resources
    public static final int HARVEST = 4;
    // contains supplies
    public static final int SUPPLY_CONTAINER = 8;
    // builds warriors
    public static final int BUILD_ARMIES = 16;
    // creates peons
    public static final int REPRODUCE = 32;
    // Can target other units
    public static final int TARGET = 64;
    // Can throw weapon
    public static final int THROW = 128;
    // Can be a rally target
    public static final int RALLY_TO = 256;
    // Can use magic
    public static final int MAGIC = 512;

    private int abilities;

    public Abilities(int abilities) {
        this.abilities = abilities;
    }

    public boolean hasAbilities(int abilities) {
        return (this.abilities | abilities) == this.abilities;
    }

    public void addAbilities(@NonNull Abilities abilities) {
        addAbilities(abilities.abilities);
    }

    public void addAbilities(int abilities) {
        this.abilities = this.abilities | abilities;
    }

    public void removeAbilities(@NonNull Abilities abilities) {
        removeAbilities(abilities.abilities);
    }

    public void removeAbilities(int abilities) {
        this.abilities = this.abilities & ~abilities;
    }
}
