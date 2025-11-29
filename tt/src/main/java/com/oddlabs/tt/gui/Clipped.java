package com.oddlabs.tt.gui;

/**
 * A marker interface for GUIObjects that should clip their contents.
 * When a Renderable object that implements this interface is rendered,
 * it will enable a scissor test for its bounds.
 */
public interface Clipped {
}
