package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.LandscapeTargetRespond;
import com.oddlabs.tt.model.weapon.DirectedThrowingWeapon;
import com.oddlabs.tt.model.weapon.RotatingThrowingWeapon;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Lightning;
import org.jspecify.annotations.NonNull;

public interface ElementVisitor {
	void visitUnit(@NonNull Unit selectable);
	void visitBuilding(@NonNull Building selectable);
	void visitEmitter(@NonNull Emitter emitter);
	void visitLightning(@NonNull Lightning lightning);
	void visitRespond(@NonNull LandscapeTargetRespond respond);
	void visitSupplyModel(@NonNull SupplyModel model);
	void visitSceneryModel(@NonNull SceneryModel model);
	void visitRubberSupply(@NonNull RubberSupply model);
	void visitDirectedThrowingWeapon(@NonNull DirectedThrowingWeapon model);
	void visitRotatingThrowingWeapon(@NonNull RotatingThrowingWeapon model);
	void visitPlants(@NonNull Plants plants);
}
