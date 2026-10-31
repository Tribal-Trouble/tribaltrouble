package com.oddlabs.converter.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * One corner of a triangle: position, vertex color, normal, two UV sets and the bones it follows. The normal may have
 * any length in the file; the game normalizes at load.
 */
public record Vertex(float x, float y, float z, float r, float g, float b, float a, float nx, float ny, float nz,
                     float u, float v, float u2, float v2,
                     @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "skin") @NonNull List<SkinWeight> skins) {

    public Vertex {
        if (skins == null)
            skins = List.of();
    }
}
