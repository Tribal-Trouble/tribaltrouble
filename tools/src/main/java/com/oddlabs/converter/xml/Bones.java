package com.oddlabs.converter.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** The bones element of a skeleton file. */
public record Bones(
                    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "bone") @NonNull List<BoneEntry> entries) {

    public Bones {
        if (entries == null)
            entries = List.of();
    }
}
