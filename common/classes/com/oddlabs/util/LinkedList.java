package com.oddlabs.util;

public final class LinkedList<T> {
	private ListElement<T> first = null;
	private ListElement<T> last = null;
	private int size = 0;

	private boolean checkParent(ListElement<T> elem) {
		if (elem.getListOwner() == null) {
			elem.setListOwner(this);
			return false;
		}
		if (elem.getListOwner() == this) return true;
		elem.getListOwner().remove(elem);
		elem.setListOwner(this);
		return false;
	}

	public void addLast(ListElement<T> elem) {
		if (checkParent(elem)) return;
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

	public void addFirst(ListElement<T> elem) {
		if (checkParent(elem)) return;
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

	public void remove(ListElement<T> element) {
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

	public void insert(ListElement<T> element, ListElement<T> next_elem) {
		if (next_elem == null) {
			addLast(element);
			return;
		}
		checkParent(element);
		assert next_elem.getListOwner() == this: "owner " + next_elem.getListOwner() + " != " + this;
		if (first == next_elem) {
			first = element;
			element.setPrior(null);
		} else {
			ListElement prev = next_elem.getPrior();
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

	public ListElement<T> getFirst() {
		return first;
	}

	public ListElement<T> getLast() {
		return last;
	}

	public void putLast(ListElement<T> element) {
		remove(element);
		addLast(element);
	}

	public void putFirst(ListElement<T> element) {
		remove(element);
		addFirst(element);
	}
}
