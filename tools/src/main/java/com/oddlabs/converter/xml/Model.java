package com.oddlabs.converter.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** A mesh file with its tint and texture tiers; a sprite's first model is the close-up one. */
public record Model(@NonNull String path, int r, int g, int b,
                    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "texture") @NonNull List<Texture> textures) {

    private static final float COLOR_SCALE = 255f;

    public Model {
        if (textures == null)
            textures = List.of();
    }

    public float @NonNull [] clearColor() {
        return new float[]{r / COLOR_SCALE, g / COLOR_SCALE, b / COLOR_SCALE};
    }
}
