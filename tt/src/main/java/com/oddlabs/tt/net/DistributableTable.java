package com.oddlabs.tt.net;

import com.oddlabs.util.HashTable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class DistributableTable {
    private final HashTable<@NonNull Distributable> distributables = new HashTable<>();
    private final Map<@NonNull Distributable, @NonNull Integer> names = new HashMap<>();
    private int current_name = 1;

    public int register(@NonNull Distributable distributable) {
        int name = current_name++;
        Distributable o = distributables.put(name, distributable);
        assert o == null : "Error registering distributable.";
        Integer p = names.put(distributable, name);
        assert p == null : "Error registering name.";
        return name;
    }

    public void unregister(@NonNull Distributable distributable) {
        Integer name = names.remove(distributable);
        assert name != null : "Error unregistering name.";

        Distributable o = distributables.remove(name);
        assert o == distributable : "Error unregistering distributable.";
    }

    public int getName(@NonNull Distributable distributable) {
        Integer val = names.get(distributable);
        assert val != null : distributable + " is not registrered.";
        return val;
    }

    public @Nullable Distributable getDistributable(int name) {
        return distributables.get(name);
    }
}
