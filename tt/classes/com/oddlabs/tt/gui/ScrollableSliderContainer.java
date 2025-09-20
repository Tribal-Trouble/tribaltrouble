package com.oddlabs.tt.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * A scrollable container for GUI groups, particularly useful for displaying many slider groups
 * in a limited vertical space. Implements the Scrollable interface to work with ScrollBar.
 */
public class ScrollableSliderContainer extends GUIObject implements Scrollable {
    private final List<Group> groups;
    private final ScrollBar scrollBar;
    private final int visibleHeight;
    private final int groupSpacing;
    private int topPadding;
    private int bottomPadding;
    private int offsetY = 0;
    private int totalContentHeight = 0;
    
    public ScrollableSliderContainer(int width, int height, int groupSpacing) {
        this.groups = new ArrayList<>();
        this.visibleHeight = height;
        this.groupSpacing = groupSpacing;
        // Initialize padding - will be set to half slider group height when first group is added
        this.topPadding = 30;    // Initial estimate (half), updated dynamically
        this.bottomPadding = 30; // Initial estimate (half), updated dynamically
        
        // Create scroll bar
        scrollBar = new ScrollBar(height, this);
        scrollBar.setPos(width - scrollBar.getWidth(), 0);
        addChild(scrollBar);
        
        setDim(width, height);
        setCanFocus(true);
    }
    
    /**
     * Add a group to the scrollable container. Groups will be stacked vertically.
     */
    public void addGroup(Group group) {
        groups.add(group);
        addChild(group);
        
        // Calculate padding based on typical font and component sizes
        if (groups.size() == 1) {
            // No top padding for tightest layout
            topPadding = 0;
            
            // No bottom padding for tightest layout
            bottomPadding = 0;
        }
        
        updateLayout();
    }
    
    /**
     * Update the layout of all groups based on current scroll offset
     *
     * Top-down layout: first group starts at the visual top of the container and
     * we position following groups beneath it. We invert the effective layout
     * offset (use -offsetY) so that:
     *  - Pressing Up (decrease offsetY) moves content up
     *  - Pressing Down (increase offsetY) moves content down
     * while keeping ordering top-to-bottom.
     */
    private void updateLayout() {
        // Effective layout offset mirrors MultiColumnComboBox top-down approach
    // Match MultiColumn top-down layout semantics: increasing offsetY moves content UP
    int layoutOffsetY = -offsetY;

    // Start from the visual top (container height) minus top padding and plus layout offset
    // Using minus layoutOffsetY effectively adds offsetY here, moving content upward for larger offsets
    int currentY = visibleHeight - topPadding - layoutOffsetY; // == visibleHeight + offsetY - topPadding

        // Compute total content height (independent of current offset)
        totalContentHeight = topPadding;

        for (Group group : groups) {
            currentY -= group.getHeight();     // move down by the group's height
            group.setPos(0, currentY);
            currentY -= groupSpacing;          // spacing below the group
            totalContentHeight += group.getHeight() + groupSpacing;
        }

        // Remove trailing spacing and add bottom padding
        if (!groups.isEmpty()) totalContentHeight -= groupSpacing;
        totalContentHeight += bottomPadding;

        scrollBar.update();
    }
    
    @Override
    protected void renderGeometry() {
        // Basic rendering - clipping will be handled by the GUI system
        // Groups will render themselves if they're positioned within the visible area
    }
    
    /**
     * Check if a group is visible in the current scroll area
     */
    private boolean isGroupVisible(Group group) {
        int groupTop = group.getY();
        int groupBottom = groupTop + group.getHeight();
        // A group is visible if any part of it overlaps with the visible area
        return groupBottom >= 0 && groupTop <= visibleHeight;
    }
    
    @Override
    protected void mouseScrolled(int amount) {
        // Normalize: wheel up => scroll up (decrease offset)
        if (amount > 0) {
            setOffsetY(offsetY - 3 * groupSpacing);
        } else {
            setOffsetY(offsetY + 3 * groupSpacing);
        }
    }
    
    // Scrollable interface implementation
    
    @Override
    public void setOffsetY(int newOffset) {
        offsetY = newOffset;

        // Clamp to valid range
        int minOffset = 0;
        if (offsetY < minOffset) offsetY = minOffset;
        // Maximum: enough to reveal the last item at the bottom of the viewport
        int maxOffset = Math.max(0, totalContentHeight - visibleHeight);
        if (offsetY > maxOffset) offsetY = maxOffset;

        updateLayout();
    }
    
    @Override
    public int getOffsetY() {
        return offsetY;
    }
    
    @Override
    public int getStepHeight() {
        // Use group spacing as step height for smooth scrolling
        return groupSpacing;
    }
    
    @Override
    public void jumpPage(boolean up) {
        if (up) {
            setOffsetY(offsetY - visibleHeight);
        } else {
            setOffsetY(offsetY + visibleHeight);
        }
    }
    
    @Override
    public float getScrollBarRatio() {
        return Math.min(1.0f, (float) visibleHeight / Math.max(totalContentHeight, visibleHeight));
    }
    
    @Override
    public float getScrollBarOffset() {
        int minOffset = 0;
        int maxOffset = Math.max(0, totalContentHeight - visibleHeight);
        int offsetRange = maxOffset - minOffset;
        if (offsetRange == 0) return 0.0f;
        return (float) (offsetY - minOffset) / offsetRange;
    }
    
    @Override
    public void setScrollBarOffset(float offset) {
        int minOffset = 0;
        int maxOffset = Math.max(0, totalContentHeight - visibleHeight);
        int offsetRange = maxOffset - minOffset;
        setOffsetY((int) (minOffset + offset * offsetRange));
    }
    
    @Override
    public void setFocus() {
        // Find first visible group and set focus to it
        for (Group group : groups) {
            if (isGroupVisible(group)) {
                group.setGroupFocus(LocalInput.isShiftDownCurrently() ? -1 : 1);
                return;
            }
        }
    }
}