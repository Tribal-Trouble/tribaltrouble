package com.oddlabs.tt.model;

/**
 * A marker interface representing client-side rendering state associated with a simulation model.
 */
public interface ClientState {
    default void update(float t) {
    }
}
