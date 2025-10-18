package com.oddlabs.util;

public interface ListElement<T> {
	public void setNext(ListElement<T> next);
	public void setPrior(ListElement<T> prior);
	public ListElement<T> getNext();
	public ListElement<T> getPrior();
	public void setListOwner(LinkedList<T> list);
	public LinkedList<T> getListOwner();
}
