package com.oddlabs.tt.model;

public final class ChieftainContainer {
    private static final float PROGRESS_AMOUNT = 1f / 40f;

    private final Building building;

    private boolean training = false;
    private float progress = 0f;

    public ChieftainContainer(Building building) {
        this.building = building;
    }

    public float getBuildProgress() {
        return progress;
    }

    public void startTraining() {
        training = true;
        ((ReproduceUnitContainer) building.getUnitContainer()).resetProgress();
    }

    public void stopTraining() {
        training = false;
        progress = 0f;
        ((ReproduceUnitContainer) building.getUnitContainer()).resetProgress();
    }

    public boolean isTraining() {
        return training;
    }

    public void progress() {
        progress += PROGRESS_AMOUNT;
        if (progress >= 1f) {
            building.deployChieftain();
        }
    }
}
