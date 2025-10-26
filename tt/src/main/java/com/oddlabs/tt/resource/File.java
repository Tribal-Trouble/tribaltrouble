package com.oddlabs.tt.resource;

import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;

import java.net.URL;
import java.util.Objects;
import java.util.function.Supplier;

public abstract class File<R> implements Supplier<R> {

    private final @NonNull URL url;

    protected File(URL url) {
        this.url = Objects.requireNonNull(url, "url");
    }

    protected File(String location) {
        this(Utils.makeURL(location));
    }

    public final URL getURL() {
        return url;
    }

    @Override
    public abstract R get();

    @Override
    public String toString() {
        return url.toString();
    }

    @Override
    public final int hashCode() {
        return url.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof File))
            return false;
        File<?> other = (File<?>) o;
        return url.equals(other.url);
    }
}
