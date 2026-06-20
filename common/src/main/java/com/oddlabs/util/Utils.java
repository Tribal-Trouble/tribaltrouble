package com.oddlabs.util;

import org.jspecify.annotations.NonNull;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.UncheckedIOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public final class Utils {
    private static final Logger logger = Logger.getLogger(Utils.class.getSimpleName());

    public static final Pattern EMAIL_PATTERN = Pattern.compile("(.+@.+\\.[a-z]+)?");
    public static final Path STD_OUT = Path.of("std.out");
    public static final Path STD_ERR = Path.of("std.err");
    public static final Path EVENT_LOG = Path.of("event.log");

    public static final Path[] LOG_FILES = {STD_OUT, STD_ERR, EVENT_LOG};

    public static @NonNull InetAddress getLoopbackAddress() {
        try {
            return tryGetLoopbackAddress();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static @NonNull InetAddress tryGetLoopbackAddress() throws IOException {
        return NetworkInterface.networkInterfaces()
                .flatMap(NetworkInterface::inetAddresses)
                .filter(InetAddress::isLoopbackAddress)
                // prefer ipv6 over ipv4
                .min(Comparator.comparingInt(addr -> addr instanceof Inet6Address ? 0 : 1))
                .stream().peek(best -> logger.info("loopback address = " + best))
                .findFirst().orElseThrow(() -> new IOException("No loopback address found"));
    }

    public static int powerOf2Log2(int n) {
        assert isPowerOf2(n) : n + " is not a power of 2";
        for (int i = 0; i < 31; i++) {
            if ((n & 1) == 1) {
                return i;
            }
            n >>= 1;
        }
        return 0;
    }

    public static boolean isPowerOf2(int n) {
        return n == 0 || (n > 0 && (n & (n - 1)) == 0);
    }

    public static int nextPowerOf2(int n) {
        int x = 1;
        while (x < n) {
            x <<= 1;
        }
        return x;
    }

    public static void flip(byte @NonNull [] bytes, int width, int height) {
        byte[] line = new byte[width];

        for (int i = 0; i < height / 2; i++) {
            System.arraycopy(bytes, i * width, line, 0, width);
            System.arraycopy(bytes, (height - i - 1) * width, bytes, i * width, width);
            System.arraycopy(line, 0, bytes, (height - i - 1) * width, width);
        }
    }

    public static void flip(@NonNull ByteBuffer bytes, int width, int height) {
        byte[] line = new byte[width];
        byte[] line2 = new byte[width];

        for (int i = 0; i < height / 2; i++) {
            bytes.position(i * width);
            bytes.get(line);
            bytes.position((height - i - 1) * width);
            bytes.get(line2);
            bytes.position(i * width);
            bytes.put(line2);
            bytes.position((height - i - 1) * width);
            bytes.put(line);
        }
    }

    public static <T> T loadObject(@NonNull Class<T> clazz, @NonNull URL url) {
        return loadObject(clazz, url, false);
    }

    public static <T> T loadObject(@NonNull Class<T> clazz, @NonNull URL url, boolean zipped) {
        try {
            return tryLoadObject(clazz, url, zipped);
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T tryLoadObject(@NonNull Class<T> clazz, @NonNull URL url) throws IOException,
            ClassNotFoundException {
        return tryLoadObject(clazz, url, false);
    }

    public static <T> T tryLoadObject(@NonNull Class<T> clazz, @NonNull URL url, boolean zipped) throws IOException,
            ClassNotFoundException {
        try (InputStream urlStream = url.openStream()) {
            try (InputStream input_stream = zipped ? new GZIPInputStream(urlStream) : new BufferedInputStream(
                    urlStream)) {
                try (ObjectInputStream obj_stream = new ObjectInputStream(input_stream)) {
                    T obj = clazz.cast(obj_stream.readObject());
                    return obj;
                }
            }
        }
    }

    public static @NonNull URI makeURI(@NonNull String location) throws UncheckedIOException {
        try {
            return makeURL(location).toURI();
        } catch (URISyntaxException e) {
            throw new UncheckedIOException("Unusable location: " + location, new IOException(e));
        }
    }

    public static @NonNull URL makeURL(@NonNull String location) throws UncheckedIOException {
        try {
            return tryMakeURL(location);
        } catch (IOException e) {
            throw new UncheckedIOException("Unusable location: " + location, e);
        }
    }

    public static @NonNull URL tryMakeURL(@NonNull String location) throws IOException {
        URL url = Utils.class.getResource(location);
        if (url == null)
            throw new IOException(location + " not found");
        return url;
    }

    private Utils() {
    }
}
