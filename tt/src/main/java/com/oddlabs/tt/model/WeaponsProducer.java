package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Manages the production of weapons in an armory building.
 */
public class WeaponsProducer {
    private static final float MAX_BREAK_TIME = .25f;
    private static final float BREAK_PROBABILITY = .2f;


    private final @NonNull Building building;
    private final @NonNull WorkerUnitContainer unit_container;
    private final @NonNull BuildProductionContainer @NonNull [] production_containers;

    private float break_time = 0f;
    private boolean producing;

    public WeaponsProducer(@NonNull Building building, @NonNull WorkerUnitContainer unit_container,
            @NonNull BuildProductionContainer @NonNull [] production_containers) {
        this.building = building;
        this.unit_container = unit_container;
        this.production_containers = production_containers;
    }

    public final boolean isProducing() {
        return producing;
    }

    public final void animate(float t) {
        Deque<@NonNull BuildProductionContainer> build_list = new ArrayDeque<>();
        for (var production_container : production_containers) {
            if (production_container.getNumSupplies() > 0 && production_container.hasEnoughSupplies()) {
                build_list.add(production_container);
            }
        }

        if (!build_list.isEmpty()) {
            if (break_time <= 0) {
                if (building.getOwner().getWorld().getRandom().nextFloat() < BREAK_PROBABILITY) {
                    break_time = building.getOwner().getWorld().getRandom().nextFloat(0f, MAX_BREAK_TIME);
                    producing = false;
                } else {
                    producing = true;
                }
            }
            float man_seconds_per_container = unit_container.getNumSupplies() * t / build_list.size();
            while (!build_list.isEmpty()) {
                build_list.pop().build(man_seconds_per_container);
            }
        } else {
            producing = false;
        }
        break_time -= t;
    }
}
