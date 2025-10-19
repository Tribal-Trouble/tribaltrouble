package com.oddlabs.tt.model;

public interface ElementNodeVisitor<T> {
	void visitNode(ElementNode<T> node);
	void visitLeaf(ElementLeaf<T> leaf);
	void visit(Element<T> element);
}
