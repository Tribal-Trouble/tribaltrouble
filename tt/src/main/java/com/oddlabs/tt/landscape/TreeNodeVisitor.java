package com.oddlabs.tt.landscape;

public interface TreeNodeVisitor {
	void visitLeaf(TreeLeaf tree_leaf);
	void visitNode(TreeGroup tree_group);
	void visitTree(TreeSupply tree_supply);
}
