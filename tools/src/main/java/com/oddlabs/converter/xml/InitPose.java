package com.oddlabs.converter.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import org.jspecify.annotations.NonNull;

import java.util.List;

/** The rest pose of a skeleton file: one transform per bone. */
public record InitPose(
                       @JacksonXmlElementWrapper(useWrapping = false) @JacksonXmlProperty(localName = "transform") @NonNull List<BoneTransform> transforms) {

    public InitPose {
        if (transforms == null)
            transforms = List.of();
    }
}
