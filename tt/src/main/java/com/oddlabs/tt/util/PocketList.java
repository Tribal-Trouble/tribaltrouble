package com.oddlabs.tt.util;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class PocketList<T> {
	private final @NonNull List<@NonNull T> @NonNull [] pockets;
	private int min_list_index;
	private int max_list_index;
	private int size;

	@SuppressWarnings("unchecked")
	public PocketList(int num_pockets) {
		pockets = IntStream.rangeClosed(0, num_pockets)
                .mapToObj(_ -> new ArrayList<T>())
                .toArray(List[]::new);
		reset();
	}

	public void add(int cost, @NonNull T obj) {
		if (cost >= pockets.length)
			cost = pockets.length - 1;
		pockets[cost].add(obj);
		if (cost < min_list_index)
			min_list_index = cost;
		if (cost > max_list_index)
			max_list_index = cost;
		size++;
	}

	public @NonNull T removeBest() {
        assert !isEmpty();
		List<T> current_pocket = pockets[min_list_index];
		while (current_pocket.isEmpty()) {
			min_list_index++;
			current_pocket = pockets[min_list_index];
		}
		T node = current_pocket.removeLast();
		size--;
		return node;
	}

	public void clear() {
		for (int i = min_list_index; i <= max_list_index; i++) {
            pockets[i].clear();
        }
//		check();
		reset();
	}

/*	private final void check() {
		for (int i = 0; i < pockets.length; i++)
			assert pockets[i].isEmpty(): min_list_index + " " + max_list_index + " " + i;
	}
*/

    public boolean isEmpty() {
        return size == 0;
    }

	public int size() {
		return size;
	}

	private void reset() {
		min_list_index = pockets.length;
		max_list_index = 0;
		size = 0;
	}
}
