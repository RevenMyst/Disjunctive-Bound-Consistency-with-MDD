package org.maxicp.state.datastructures;

import org.maxicp.state.StateInt;
import org.maxicp.state.StateManager;
import org.maxicp.util.exception.NotImplementedException;

import java.util.*;

public class StateDoubleLinkedList<T> implements List<T> {

    protected StateManager sm;

    protected StateInt size;
    protected StateDoubleLinkedListNode<T> head;
    protected StateDoubleLinkedListNode<T> tail;

    public StateDoubleLinkedList(StateManager sm) {
        this.sm = sm;
        this.size = this.sm.makeStateInt(0);
        this.head = new StateDoubleLinkedListNode<>(this.sm, null); // dummy start node
        this.tail = new StateDoubleLinkedListNode<>(this.sm, null); // dummy end node
        this.head.next.setValue(tail);
        this.tail.previous.setValue(head);
    }

    protected StateDoubleLinkedListNode<T> getNode(T t) {
        return new StateDoubleLinkedListNode<>(this.sm, t);
    }

    @Override
    public boolean add(T t) {
        StateDoubleLinkedListNode<T> h = this.getNode(t);
        h.previous.setValue(tail.previous.value());
        h.next.setValue(this.tail);
        this.tail.previous.setValue(h);
        h.previous.value().next.setValue(h);
        size.increment();
        return true;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        StateDoubleLinkedListNode<T> current = head.next.value();
        int i = 0;
        while (i < size.value()) {
            sb.append(current).append(" ");
            current = current.next.value();
            i++;
        }
        return sb.toString();
    }

    @Override
    public int size() {
        return size.value();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        StateDoubleLinkedListNode<T> current = head;
        int i = 0;
        while (i < this.size()) {
            current = current.next.value();
            if (current.element.equals(o))
                return true;
            i++;
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            StateDoubleLinkedListNode<T> current = head.next.value();

            @Override
            public boolean hasNext() {
                return current != tail && current != null;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T value = current.element;
                current = current.next.value();
                return value;
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] array = new Object[size.value()];
        StateDoubleLinkedListNode<T> current = head.next.value();
        int i = 0;
        while (i < size.value()) {
            array[i] = current.element;
            current = current.next.value();
            i++;
        }
        return array;
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        if (a.length < size()) {
            a = (T1[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size.value());
        }
        StateDoubleLinkedListNode<T> current = head.next.value();
        int i = 0;
        while (i < size.value()) {
            a[i] = (T1) current.element;
            current = current.next.value();
            i++;
        }
        if (i < a.length) {
            a[i] = null;
        }
        return a;
    }


    @Override
    public boolean remove(Object o) {
        if (o == null) return false;
        StateDoubleLinkedListNode<T> current = head.next.value();
        while (current!= tail && current.element.equals(o)) {
            current = current.next.value();
        }
        if (current == tail)
            return false;
        StateDoubleLinkedListNode<T> previous = current.previous.value();
        StateDoubleLinkedListNode<T> next = current.next.value();
        previous.next.setValue(next);
        next.previous.setValue(previous);
        size.decrement();
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object value : c) {
            if (!contains(value)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void clear() {
        head.next.setValue(tail);
        tail.previous.setValue(head);
        size.setValue(0);
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size.value()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size.value());
        }
        StateDoubleLinkedListNode<T> current = head.next.value();
        for (int i = 0; i < index; i++) {
            current = current.next.value();
        }
        return current.element;
    }

    @Override
    public T set(int index, T element) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public void add(int index, T element) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public T remove(int index) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public int indexOf(Object o) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new NotImplementedException("Not implemented");
    }
}
