package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

public final class Abilities {
    // No abilities
    public final static int NONE = 0;
    // Can build/repair buildings
    public final static int BUILD = 1;
    // Can attack other units
    public final static int ATTACK = 2;
    // Can harvest resources
    public final static int HARVEST = 4;
    // contains supplies
    public final static int SUPPLY_CONTAINER = 8;
    // builds warriors
    public final static int BUILD_ARMIES = 16;
    // creates peons
    public final static int REPRODUCE = 32;
    // Can target other units
    public final static int TARGET = 64;
    // Can throw weapon
    public final static int THROW = 128;
    // Can be a rally target
    public final static int RALLY_TO = 256;
    // Can use magic
    public final static int MAGIC = 512;

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
