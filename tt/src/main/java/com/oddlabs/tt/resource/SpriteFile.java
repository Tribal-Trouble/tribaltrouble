package com.oddlabs.tt.resource;

import com.oddlabs.tt.render.SpriteList;
import org.jspecify.annotations.NonNull;

public final class SpriteFile extends File<SpriteList> {
	private final boolean lighting;
	private final boolean cullface;
	private final boolean alpha;
	private final boolean modulate_color;
	private final boolean max_alpha;
	private final int mipmap_cutoff;

	public SpriteFile(@NonNull String location, int mipmap_cutoff, boolean lighting, boolean cullface, boolean alpha, boolean modulate_color) {
		this(location, mipmap_cutoff, lighting, cullface, alpha, modulate_color, false);
	}

	public SpriteFile(@NonNull String location, int mipmap_cutoff, boolean lighting, boolean cullface, boolean alpha, boolean modulate_color, boolean max_alpha) {
		super(location);
		this.lighting = lighting;
		this.cullface = cullface;
		this.alpha = alpha;
		this.modulate_color = modulate_color;
		this.mipmap_cutoff = mipmap_cutoff;
		this.max_alpha = max_alpha;
	}

    @Override
	public @NonNull SpriteList get() {
		return new SpriteList(this);
	}

    @Override
	public boolean equals(Object o) {
		if (!(o instanceof SpriteFile))
			return false;
		SpriteFile other = (SpriteFile)o;
		if (mipmap_cutoff != other.mipmap_cutoff || lighting != other.lighting || cullface != other.cullface || alpha != other.alpha || modulate_color != other.modulate_color)
			return false;
		return super.equals(o);
	}

	public int getMipmapCutoff() {
		return mipmap_cutoff;
	}

	public boolean isLighted() {
		return lighting;
	}

	public boolean isCulled() {
		return cullface;
	}

	public boolean hasAlpha() {
		return alpha;
	}

	public boolean hasModulateColor() {
		return modulate_color;
	}

	public boolean hasMaxAlpha() {
		return max_alpha;
	}
}
