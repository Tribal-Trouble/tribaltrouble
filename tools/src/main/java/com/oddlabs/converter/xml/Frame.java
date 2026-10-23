package com.oddlabs.converter.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** One frame of a clip, numbered from 0. */
public record Frame(int index,
                    @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "transform") @NonNull List<BoneTransform> transforms) {

    public Frame {
        if (index < 0)
            throw new IllegalArgumentException("Frame index " + index + " is negative");
        if (transforms == null)
            transforms = List.of();
    }
}
