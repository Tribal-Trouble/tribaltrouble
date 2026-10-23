package com.oddlabs.converter.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** The whole of geometry.xml, one record per element. */
public record GeometryFile(@JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "group") @NonNull List<Group> groups) {

    public GeometryFile {
        if (groups == null)
            groups = List.of();
    }

    public static @NonNull GeometryFile read(@NonNull Path file) throws IOException {
        return Xml.read(file, GeometryFile.class);
    }
}
