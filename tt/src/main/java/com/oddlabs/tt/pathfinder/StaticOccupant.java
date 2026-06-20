package com.oddlabs.tt.pathfinder;

public final class StaticOccupant implements Occupant {
    @Override
    public int getPenalty() {
        return Occupant.STATIC;
    }

    @Override
    public int getGridX() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getGridY() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getPositionX() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getPositionY() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getSize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDead() {
        throw new UnsupportedOperationException();
    }

    public void startRespond() {
        throw new UnsupportedOperationException();
    }

    public void stopRespond() {
        throw new UnsupportedOperationException();
    }
}
