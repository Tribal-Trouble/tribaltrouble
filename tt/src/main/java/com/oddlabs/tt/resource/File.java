package com.oddlabs.tt.resource;

import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Supplier;

public abstract class File<R> implements Supplier<R> {

    private final @NonNull URI uri;

    protected File(@NonNull URL url) {
        URI asURI;
        try {
            asURI = url.toURI();
        } catch (URISyntaxException e) {
            throw new UncheckedIOException(new IOException("unusable location: " + url, e));
        }
        this(asURI);
    }

    public File(@NonNull URI uri) {
        this.uri = uri;
    }

    protected File(@NonNull String location) {
        this(Utils.makeURI(location));
    }

    public final @NonNull URL getURL() {
        try {
            return uri.toURL();
        } catch (MalformedURLException e) {
            throw new UncheckedIOException(new IOException("bad location: " + uri, e));
        }
    }

    @Override
    public abstract R get();

    @Override
    public String toString() {
        return uri.toASCIIString();
    }

    @Override
    public final int hashCode() {
        return uri.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof File<?> other))
            return false;
        return uri.equals(other.uri);
    }
}
