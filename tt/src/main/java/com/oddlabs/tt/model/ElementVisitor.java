package com.oddlabs.tt.model;

import com.oddlabs.tt.landscape.LandscapeTargetRespond;
import com.oddlabs.tt.model.weapon.DirectedThrowingWeapon;
import com.oddlabs.tt.model.weapon.RotatingThrowingWeapon;
import com.oddlabs.tt.particle.Emitter;
import com.oddlabs.tt.particle.Lightning;

public interface ElementVisitor {
	void visitUnit(Unit selectable);
	void visitBuilding(Building selectable);
	void visitEmitter(Emitter emitter);
	void visitLightning(Lightning lightning);
	void visitRespond(LandscapeTargetRespond respond);
	void visitSupplyModel(SupplyModel model);
	void visitSceneryModel(SceneryModel model);
	void visitRubberSupply(RubberSupply model);
	void visitDirectedThrowingWeapon(DirectedThrowingWeapon model);
	void visitRotatingThrowingWeapon(RotatingThrowingWeapon model);
	void visitPlants(Plants plants);
}
