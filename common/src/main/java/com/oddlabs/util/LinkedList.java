package com.oddlabs.util;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class LinkedList<T extends ListElement<T>> {
    private @Nullable T first;
    private @Nullable T last;
    private int size = 0;

    /**
     * {@return true if element is already in list otherwise false}
     */
    private boolean checkOwner(@NonNull T elem) {
        if (elem.getListOwner() == this) return true;
        if (elem.getListOwner() != null) {
            elem.getListOwner().remove(elem);
        }
        elem.setListOwner(this);
        return false;
    }

    public void addLast(@NonNull T elem) {
        if (checkOwner(elem)) return;
        if (last == null) {
            first = elem;
            last = elem;
            elem.setNext(null);
            elem.setPrior(null);
        } else {
            elem.setNext(null);
            elem.setPrior(last);
            last.setNext(elem);
            last = elem;
        }
        size++;
    }

    public void addFirst(@NonNull T elem) {
        if (checkOwner(elem)) return;
        if (last == null) {
            first = elem;
            last = elem;
            elem.setNext(null);
            elem.setPrior(null);
        } else {
            elem.setNext(first);
            elem.setPrior(null);
            first.setPrior(elem);
            first = elem;
        }
        size++;
    }

    public void remove(@NonNull T element) {
        assert element.getListOwner() == this;
        element.setListOwner(null);
        if (last == element && first == element) {
            first = null;
            last = null;
        } else if (last == element) {
            last = element.getPrior();
            last.setNext(null);
        } else if (first == element) {
            first = element.getNext();
            first.setPrior(null);
        } else {
            element.getPrior().setNext(element.getNext());
            element.getNext().setPrior(element.getPrior());
        }
        size--;
    }

    public void insert(@NonNull T element, @Nullable T next_elem) {
        if (next_elem == null) {
            addLast(element);
            return;
        }
        checkOwner(element);
        assert next_elem.getListOwner() == this : "owner " + next_elem.getListOwner() + " != " + this;
        if (first == next_elem) {
            first = element;
            element.setPrior(null);
        } else {
            T prev = next_elem.getPrior();
            element.setPrior(prev);
            prev.setNext(element);
        }
        next_elem.setPrior(element);
        element.setNext(next_elem);
        size++;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public @Nullable T getFirst() {
        return first;
    }

    public @Nullable T getLast() {
        return last;
    }

    public void putLast(@NonNull T element) {
        remove(element);
        addLast(element);
    }

    public void putFirst(@NonNull T element) {
        remove(element);
        addFirst(element);
    }
}
