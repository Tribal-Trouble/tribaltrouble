package com.oddlabs.net;

import org.jspecify.annotations.NonNull;

import java.util.Comparator;

public final class MethodComparator implements Comparator<Object> {
    @Override
    public int compare(@NonNull Object o1, @NonNull Object o2) {
        return o1.toString().compareTo(o2.toString());
    }
}
