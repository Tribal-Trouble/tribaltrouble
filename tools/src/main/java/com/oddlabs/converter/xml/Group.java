package com.oddlabs.converter.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** A folder of related sprites: vikings, natives, misc. */
public record Group(@NonNull String name,
                    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "sprite") @NonNull List<Sprite> sprites) {

    public Group {
        if (sprites == null)
            sprites = List.of();
    }
}
