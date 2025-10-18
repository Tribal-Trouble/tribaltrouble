package com.oddlabs.util;

public interface ListElement<T> {
	void setNext(ListElement<T> next);
	void setPrior(ListElement<T> prior);
	ListElement<T> getNext();
	ListElement<T> getPrior();
	void setListOwner(LinkedList<T> list);
	LinkedList<T> getListOwner();
}
