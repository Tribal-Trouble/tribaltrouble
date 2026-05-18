package com.oddlabs.tt.player.campaign;

import org.jspecify.annotations.NonNull;

import java.io.Serial;
import java.io.Serializable;

public final class CampaignState implements Serializable {
    @Serial
    private static final long serialVersionUID = 1;

    public static final int RACE_VIKINGS = 0; // DON'T CHANGE! will ruin serializability
    public static final int RACE_NATIVES = 1;

    public static final int DIFFICULTY_EASY = 1;
    public static final int DIFFICULTY_NORMAL = 0; //Serializable defaults to 0
    public static final int DIFFICULTY_HARD = 2;

    public static final int ISLAND_AVAILABLE = 1;
    public static final int ISLAND_UNAVAILABLE = 2;
    public static final int ISLAND_COMPLETED = 3;
    public static final int ISLAND_SEMI_AVAILABLE = 4;
    public static final int ISLAND_HIDDEN = 5;

    private final int @NonNull [] island_states;

    private int prev_island;
    private int current_island = -1;
    private int num_peons = 10;
    private int num_rock_warriors = 0;
    private int num_iron_warriors = 0;
    private int num_rubber_warriors = 0;
    private boolean has_rubber_weapons = false;
    private boolean has_magic0 = false;
    private boolean has_magic1 = false;

    private @NonNull String name;
    private long date;
    private int race;
    private int difficulty;

    public CampaignState(int @NonNull [] initial_states) {
        island_states = new int[initial_states.length];
        System.arraycopy(initial_states, 0, island_states, 0, island_states.length);
        name = "";
    }

    public void setIslandState(int index, int state) {
        if (island_states[index] == ISLAND_SEMI_AVAILABLE && state == ISLAND_SEMI_AVAILABLE)
            island_states[index] = ISLAND_AVAILABLE;
        else if (island_states[index] != ISLAND_COMPLETED)
            island_states[index] = state;
    }

    public int getIslandState(int index) {
        /*		for (int i = 0; i < island_states.length; i++)
        			island_states[i] = ISLAND_AVAILABLE;
        		island_states[0] = ISLAND_COMPLETED;*/
        return island_states[index];
    }

    public void setCurrentIsland(int current_island) {
        setPrevIsland(this.current_island);
        this.current_island = current_island;
    }

    public int getCurrentIsland() {
        return current_island;
    }

    public void setPrevIsland(int prev_island) {
        this.prev_island = prev_island;
    }

    public int getPrevIsland() {
        return prev_island;
    }

    public void setNumPeons(int num_peons) {
        this.num_peons = num_peons;
    }

    public int getNumPeons() {
        return num_peons;
    }

    public void setNumRockWarriors(int num_rock_warriors) {
        this.num_rock_warriors = num_rock_warriors;
    }

    public int getNumRockWarriors() {
        return num_rock_warriors;
    }

    public void setNumIronWarriors(int num_iron_warriors) {
        this.num_iron_warriors = num_iron_warriors;
    }

    public int getNumIronWarriors() {
        return num_iron_warriors;
    }

    public void setNumRubberWarriors(int num_rubber_warriors) {
        this.num_rubber_warriors = num_rubber_warriors;
    }

    public int getNumRubberWarriors() {
        return num_rubber_warriors;
    }

    public void setHasRubberWeapons(boolean has_rubber_weapons) {
        this.has_rubber_weapons = has_rubber_weapons;
    }

    public boolean hasRubberWeapons() {
        return has_rubber_weapons;
    }

    public void setHasMagic0(boolean has_magic0) {
        this.has_magic0 = has_magic0;
    }

    public boolean hasMagic0() {
        return has_magic0;
    }

    public void setHasMagic1(boolean has_magic1) {
        this.has_magic1 = has_magic1;
    }

    public boolean hasMagic1() {
        return has_magic1;
    }

    public @NonNull String getName() {
        return name;
    }

    public void setName(@NonNull String name) {
        this.name = name;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public void setRace(int race) {
        this.race = race;
    }

    public int getRace() {
        return race;
    }

    public void setDifficulty(int difficulty) {
        this.difficulty = difficulty;
    }

    public int getDifficulty() {
        return difficulty;
    }
}
