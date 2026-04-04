package com.oddlabs.tt.landscape;

public interface PatchGroupVisitor {
    void visitGroup(PatchGroup group);

    void visitLeaf(LandscapeLeaf leaf);
}
