package com.oddlabs.tt.resource;

import com.oddlabs.util.Utils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

public abstract class File<R> implements Supplier<R> {

    private final @NonNull URI uri;

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
    public abstract @NonNull R get();

    @Override
    public final int hashCode() {
        return uri.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        return o instanceof File<?> other && uri.equals(other.uri);
    }

    @Override
    public @NonNull String toString() {
        return getClass().getSimpleName() + "{uri=" + uri.toASCIIString() + '}';
    }

    protected static @NonNull Optional<URI> locate(@NonNull String location) {
        URL url_classpath = Utils.class.getResource(location);
        if (url_classpath != null) try {
            return Optional.of(url_classpath.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        Path file = com.oddlabs.tt.util.Utils.getInstallDir().resolve(location);
        return Files.isRegularFile(file) && Files.isReadable(file) ? Optional.of(file.toUri()) : Optional.empty();
    }
}
