package com.oddlabs.tt.model;

import org.jspecify.annotations.NonNull;

import java.util.LinkedHashSet;
import java.util.Set;

public class Army {
	private final Set<@NonNull Selectable<?>> selection = new LinkedHashSet<>();

	public final Selectable<?>[] filter(int ability_filter) {
        return (Selectable<?>[]) selection.stream()
                .filter(s -> s.getAbilities().hasAbilities(ability_filter))
                .toArray(Selectable[]::new);
	}

	public final boolean containsAbility(int ability_filter) {
        return selection.stream()
                .anyMatch(s -> s.getAbilities().hasAbilities(ability_filter));
	}

	public void clear() {
		selection.clear();
	}

	public void remove(@NonNull Selectable<?> selectable) {
		selection.remove(selectable);
	}

	public final boolean contains(@NonNull Selectable<?> selectable) {
		return selection.contains(selectable);
	}

	public final @NonNull Set<@NonNull Selectable<?>> getSet() {
		return selection;
	}

	public void add(@NonNull Selectable<?> selectable) {
		selection.add(selectable);
	}

	public final int size() {
		return selection.size();
	}
}
