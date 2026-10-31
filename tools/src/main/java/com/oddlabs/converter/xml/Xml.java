package com.oddlabs.converter.xml;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.jspecify.annotations.NonNull;

import javax.xml.stream.XMLInputFactory;
import java.io.IOException;
import java.nio.file.Path;

/** Reads one of the asset XML files into its record, validating it against the DTD the file carries first. */
public final class Xml {
    private static final XmlMapper MAPPER = mapper();

    private Xml() {
    }

    public static <T> @NonNull T read(@NonNull Path file, @NonNull Class<T> type) throws IOException {
        return MAPPER.readValue(file.toFile(), type);
    }

    private static @NonNull XmlMapper mapper() {
        XmlMapper mapper = XmlMapper.builder().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS).build();
        // The files are ours and their DTDs are inline, so the parser may read and enforce them.
        mapper.getFactory().getXMLInputFactory().setProperty(XMLInputFactory.SUPPORT_DTD, true);
        mapper.getFactory().getXMLInputFactory().setProperty(XMLInputFactory.IS_VALIDATING, true);
        return mapper;
    }
}
