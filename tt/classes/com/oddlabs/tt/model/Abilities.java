package com.oddlabs.tt.model;

public final strictfp class Abilities {
    public static final int NONE = 0;
    public static final int BUILD = 1;
    public static final int ATTACK = 2;
    public static final int HARVEST = 4;
    public static final int SUPPLY_CONTAINER = 8;
    public static final int BUILD_ARMIES = 16;
    public static final int REPRODUCE = 32;
    public static final int TARGET = 64;
    public static final int THROW = 128;
    public static final int RALLY_TO = 256;
    public static final int MAGIC = 512;
    public static final int SAIL = 1024;

    private int abilities;

    public Abilities(int abilities) {
        this.abilities = abilities;
    }

    public final boolean hasAbilities(int abilities) {
        return (this.abilities | abilities) == this.abilities;
    }

    public final void addAbilities(Abilities abilities) {
        addAbilities(abilities.abilities);
    }

    public final void addAbilities(int abilities) {
        this.abilities = this.abilities | abilities;
    }

    public final void removeAbilities(Abilities abilities) {
        removeAbilities(abilities.abilities);
    }

    public final void removeAbilities(int abilities) {
        this.abilities = this.abilities & ~abilities;
    }
}
