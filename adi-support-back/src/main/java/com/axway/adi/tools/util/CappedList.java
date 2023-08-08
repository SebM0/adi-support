package com.axway.adi.tools.util;

import java.util.*;

/**
 * Linked List that supports a limited number of elements
 * Element must be inserted at the end
 * Exceeding elements are ejected according FIFO
 *
 * @param <E> the contained element
 */
public class CappedList<E> extends LinkedList<E> {
    private final int m_maxSize;

    public CappedList(int maxSize) {
        m_maxSize = maxSize;
    }

    @Override
    public void addFirst(E e) {
        throw new UnsupportedOperationException("Insert elements by the end");
    }

    @Override
    public void addLast(E e) {
        add(e);
    }

    @Override
    public boolean add(E e) {
        super.add(e);
        while (size() > m_maxSize) {
            removeFirst();
        }
        return true;
    }
}
