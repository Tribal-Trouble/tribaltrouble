package com.oddlabs.tt.model;

public interface ElementNodeVisitor {
	void visitNode(ElementNode node);
	void visitLeaf(ElementLeaf leaf);
	void visit(Element<?> element);
}
