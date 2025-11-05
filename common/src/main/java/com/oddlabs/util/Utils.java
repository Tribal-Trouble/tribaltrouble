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
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

public final class Utils {
	public final static Pattern EMAIL_PATTERN = Pattern.compile("(.+@.+\\.[a-z]+)?");
	public final static Path STD_OUT = Path.of("std.out");
	public final static Path STD_ERR = Path.of("std.err");
	public final static Path EVENT_LOG = Path.of("event.log");

	public final static Path[] LOG_FILES = {STD_OUT, STD_ERR, EVENT_LOG};

	public static @NonNull InetAddress getLoopbackAddress() {
		try {
			return tryGetLoopbackAddress();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static @NonNull InetAddress tryGetLoopbackAddress() throws IOException {
		Enumeration<NetworkInterface> interfaces;
		interfaces = NetworkInterface.getNetworkInterfaces();
		InetAddress best_address = null;
out:
		while (interfaces.hasMoreElements()) {
			NetworkInterface network_interface = interfaces.nextElement();
			Enumeration<InetAddress> addresses = network_interface.getInetAddresses();
			while (addresses.hasMoreElements()) {
				InetAddress address = addresses.nextElement();
				if (address.isLoopbackAddress()) {
					best_address = address;
					// Prefer ipv4 addresses because of BUG 6230761
					if (!(address instanceof Inet6Address))
						break out;
				}
			}
		}
		if (best_address != null) {
System.out.println("loopback address = " + best_address);
			return best_address;
		}
		throw new IOException("Could not find a loopback address");
	}

	public static int powerOf2Log2(int n) {
		assert isPowerOf2(n): n + " is not a power of 2";
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

		for (int i = 0; i < height/2; i++) {
			System.arraycopy(bytes, i*width, line, 0, width);
			System.arraycopy(bytes, (height - i - 1)*width, bytes, i*width, width);
			System.arraycopy(line, 0, bytes, (height - i - 1)*width, width);
		}
	}

	public static void flip(@NonNull ByteBuffer bytes, int width, int height) {
		byte[] line = new byte[width];
		byte[] line2 = new byte[width];

		for (int i = 0; i < height/2; i++) {
			bytes.position(i*width);
			bytes.get(line);
			bytes.position((height - i - 1)*width);
			bytes.get(line2);
			bytes.position(i*width);
			bytes.put(line2);
			bytes.position((height - i - 1)*width);
			bytes.put(line);
		}
	}

	public static <T> T loadObject(@NonNull URL url) {
		return loadObject(url, false);
	}

	public static <T> T loadObject(@NonNull URL url, boolean zipped) {
		try {
			return tryLoadObject(url, zipped);
		} catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        } catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public static <T> T tryLoadObject(@NonNull URL url) throws IOException, ClassNotFoundException {
		return tryLoadObject(url, false);
	}

	public static <T> T tryLoadObject(@NonNull URL url, boolean zipped) throws IOException, ClassNotFoundException {
        try (InputStream urlStream = url.openStream()) {
            try (InputStream input_stream = zipped ? new GZIPInputStream(urlStream) : new BufferedInputStream(urlStream)) {
                try (ObjectInputStream obj_stream = new ObjectInputStream(input_stream)) {
                    @SuppressWarnings("unchecked")
                    T obj = (T) obj_stream.readObject();
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
