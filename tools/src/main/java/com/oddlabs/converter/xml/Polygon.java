package com.oddlabs.converter.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** One triangle; the DTD allows exactly three vertices. */
public record Polygon(
                      @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "vertex") @NonNull List<Vertex> vertices) {
    public static final int CORNERS = 3;

    public Polygon {
        if (vertices == null || vertices.size() != CORNERS)
            throw new IllegalArgumentException(
                    "Mesh not triangulated: a polygon has " + (vertices == null ? 0 : vertices.size()) + " vertices");
    }
}
