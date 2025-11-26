package com.oddlabs.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FontInfo implements Serializable {
	@Serial
	private static final long serialVersionUID = 1;

	private final @NonNull String texture_name;
	private final @Nullable Quad @NonNull [] key_map;
	private final int x_border;
	private final int y_border;
	private final int font_height;

	public FontInfo(@NonNull String texture_name, @Nullable Quad @NonNull[] key_map, int x_border, int y_border, int font_height) {
		this.texture_name = texture_name;
		this.key_map = key_map;
		this.x_border = x_border;
		this.y_border = y_border;
		this.font_height = font_height;
	}

	public @NonNull String getTextureName() {
		return texture_name;
	}

	public @Nullable Quad @NonNull [] getKeyMap() {
		return key_map;
	}

	public int getBorderX() {
		return x_border;
	}

	public int getBorderY() {
		return y_border;
	}

	public int getHeight() {
		return font_height;
	}

	public void saveToFile(@NonNull Path file_name) {
		try (ObjectOutputStream os = new ObjectOutputStream(new BufferedOutputStream(Files.newOutputStream(file_name)))) {
			os.writeObject(this);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static FontInfo loadFromFile(@NonNull URL url) {
		return Utils.loadObject(url);
	}
}
