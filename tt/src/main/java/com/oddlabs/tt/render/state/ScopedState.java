package com.oddlabs.tt.render.state;

public interface ScopedState extends AutoCloseable {
    @Override
    void close();
}
