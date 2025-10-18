package com.oddlabs.tt.landscape;

public final class TreeLeaf extends AbstractTreeGroup {
	private TreeSupply[] infos = new TreeSupply[0];

	public TreeLeaf(AbstractTreeGroup parent) {
		super(parent);
	}

	void insertTree(TreeSupply tree) {
		TreeSupply[] new_infos = new TreeSupply[infos.length + 1];
		System.arraycopy(infos, 0, new_infos, 0, infos.length);
		new_infos[new_infos.length - 1] = tree;
		infos = new_infos;
	}

    @Override
	protected boolean initBounds() {
		if (infos.length != 0) {
			TreeSupply info = infos[0];
			info.initBounds();
			setBounds(info);
			for (int i = 1; i < infos.length; i++) {
				info = infos[i];
				info.initBounds();
				checkBounds(info);
			}
			super.initBounds();
			return true;
		}
		return false;
	}

    @Override
	public void visit(TreeNodeVisitor visitor) {
		visitor.visitLeaf(this);
	}

	public void visitTrees(TreeNodeVisitor visitor) {
        for (TreeSupply info : infos) {
            visitor.visitTree(info);
        }
	}
}
