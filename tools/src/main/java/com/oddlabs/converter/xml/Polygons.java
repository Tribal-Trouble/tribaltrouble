package com.oddlabs.converter.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** The polygons element of a mesh file. */
public record Polygons(
                       @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "polygon") @NonNull List<Polygon> triangles) {

    public Polygons {
        if (triangles == null)
            triangles = List.of();
    }
}
