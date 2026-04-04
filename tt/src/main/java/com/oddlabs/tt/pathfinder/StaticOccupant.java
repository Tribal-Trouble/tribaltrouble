package com.oddlabs.tt.pathfinder;

public final class StaticOccupant implements Occupant {
    @Override
    public int getPenalty() {
        return Occupant.STATIC;
    }

    @Override
    public int getGridX() {
        throw new RuntimeException();
    }

    @Override
    public int getGridY() {
        throw new RuntimeException();
    }

    @Override
    public float getPositionX() {
        throw new RuntimeException();
    }

    @Override
    public float getPositionY() {
        throw new RuntimeException();
    }

    @Override
    public float getSize() {
        throw new RuntimeException();
    }

    @Override
    public boolean isDead() {
        throw new RuntimeException();
    }

    public void startRespond() {
        throw new RuntimeException();
    }

    public void stopRespond() {
        throw new RuntimeException();
    }
}
