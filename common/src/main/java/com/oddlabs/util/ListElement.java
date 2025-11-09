package com.oddlabs.util;

import org.jspecify.annotations.Nullable;

public interface ListElement<T> {
	void setNext(ListElement<T> next);
	void setPrior(ListElement<T> prior);
	@Nullable ListElement<T> getNext();
	@Nullable ListElement<T> getPrior();
	void setListOwner(LinkedList<T> list);
	@Nullable LinkedList<T> getListOwner();
}
