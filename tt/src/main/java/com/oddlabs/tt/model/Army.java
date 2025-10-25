package com.oddlabs.tt.model;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class Army {
	private final Set<Selectable> selection = new LinkedHashSet<>();

	public final Selectable[] filter(int ability_filter) {
		int count = 0;
		Iterator<Selectable> it = selection.iterator();
		while (it.hasNext()) {
			Selectable s = it.next();
			if (s.getAbilities().hasAbilities(ability_filter))
				count++;
		}
		Selectable[] filtered = new Selectable[count];
		it = selection.iterator();
		int index = 0;
		while (it.hasNext()) {
			Selectable s = it.next();
			if (s.getAbilities().hasAbilities(ability_filter))
				filtered[index++] = s;
		}
		return filtered;
	}

	public final boolean containsAbility(int ability_filter) {
        for (Selectable s : selection) {
            if (s.getAbilities().hasAbilities(ability_filter))
                return true;
        }
		return false;
	}

	public void clear() {
		selection.clear();
	}

	public void remove(Selectable selectable) {
		selection.remove(selectable);
	}

	public final boolean contains(Selectable selectable) {
		return selection.contains(selectable);
	}

	public final Set<Selectable> getSet() {
		return selection;
	}

	public void add(Selectable selectable) {
		selection.add(selectable);
	}

	public final int size() {
		return selection.size();
	}
}
