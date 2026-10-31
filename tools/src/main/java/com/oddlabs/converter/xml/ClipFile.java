package com.oddlabs.converter.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** A clip file: numbered frames, each with one transform per bone. */
public record ClipFile(
                       @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "frame") @NonNull List<Frame> frames) {

    public ClipFile {
        if (frames == null)
            frames = List.of();
    }
}
