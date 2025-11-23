package com.oddlabs.tt.model;

public interface ElementNodeVisitor<T extends Element<T>> {
	void visitNode(ElementNode<T> node);
	void visitLeaf(ElementLeaf<T> leaf);
	void visit(T element);
}
