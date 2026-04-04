package com.oddlabs.util;

import org.jspecify.annotations.Nullable;

public interface ListElement<T extends ListElement<T>> {
    @Nullable T getPrior();

    void setPrior(@Nullable T prior);

    @Nullable T getNext();

    void setNext(@Nullable T next);

    /**
     * {@return list to which this element belongs or null if none}
     */
    @Nullable LinkedList<T> getListOwner();

    void setListOwner(@Nullable LinkedList<T> list);
}
