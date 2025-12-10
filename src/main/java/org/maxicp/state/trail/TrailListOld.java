package org.maxicp.state.trail;

import org.maxicp.state.StateEntry;
import org.maxicp.state.StateInt;
import org.maxicp.state.StateListOld;

import java.util.*;

public class TrailListOld<T> implements StateListOld<T> {


    public class TrailListElement<T>{
        private T value;
        private TrailListElement<T> next;
        private TrailListElement<T> prev;

        public TrailListElement(T value) {
            this.value = value;
            this.next = null;
            this.prev = null;
        }

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
        }
        public void unlink() {
            if (prev != null) {
                prev.next = next;
            }
            if (next != null) {
                next.prev = prev;
            }
            prev = null;
            next = null;
        }
    }

    Trailer trail;
    StateInt size;
    TrailListElement<T> head;
    TrailListElement<T> tail;

    public TrailListOld(Trailer trail) {
        this.size = trail.makeStateInt(0);
        this.head = new TrailListElement<>(null);
        this.tail = new TrailListElement<>(null);
        this.head.next = tail;
        this.tail.prev = head;
        this.trail = trail;
    }

    public boolean add(T value) {
        TrailListElement<T> newElement = new TrailListElement<>(value);
        tail.prev.next = newElement;
        newElement.prev = tail.prev;
        newElement.next = tail;
        tail.prev = newElement;

        TrailListElement<T> prev = newElement.prev;

        trail.pushState(new StateEntry() {
            @Override
            public void restore() {
                if (prev != null) {
                    prev.next = tail;
                    tail.prev = prev;
                }
                // at this point, newElement is no longer referenced *at all*
            }
        });

        size.setValue(size.value() + 1);
        return true;
    }


    public String toString() {
        StringBuilder sb = new StringBuilder();
        TrailListElement<T> current = head.next;
        int i = 0;
        while (i < size.value()) {
            sb.append(current.getValue()).append(" ");
            current = current.next;
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
        TrailListElement<T> current = head.next;
        int i = 0;
        while (i < size.value()) {
            if (current.getValue() == o) {
                return true;
            }
            current = current.next;
            i++;
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private TrailListElement<T> current = head.next;

            @Override
            public boolean hasNext() {
                return current != tail;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T value = current.getValue();
                current = current.next;
                return value;
            }
        };
    }

    @Override
    public Object[] toArray() {
        Object[] array = new Object[size.value()];
        TrailListElement<T> current = head.next;
        int i = 0;
        while (i < size.value()) {
            array[i] = current.getValue();
            current = current.next;
            i++;
        }
        return array;
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        if(a.length < size.value()) {
            a = (T1[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size.value());
        }
        TrailListElement<T> current = head.next;
        int i = 0;
        while (i < size.value()) {
            a[i] = (T1) current.getValue();
            current = current.next;
            i++;
        }
        if (i < a.length) {
            a[i] = null;
        }
        return a;
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


    private void insertList(TrailListElement<T> before, Collection<? extends T> c){
        TrailListElement<T> newHead = before;
        TrailListElement<T> newTail = before.next;
        for (T value : c) {
            TrailListElement<T> newElement = new TrailListElement<>(value);
            newHead.next = newElement;
            newElement.prev = newHead;
            newHead = newElement;
        }
        newHead.next = newTail;
        newTail.prev = newHead;
        trail.pushState(new StateEntry() {
            @Override
            public void restore() {
                before.next = newTail;
                newTail.prev = before;
            }
        });

        size.setValue(size.value() + c.size());

    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        insertList(tail.prev, c);
        return true;
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> c) {
        if (index < 0 || index > size.value()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size.value());
        }
        TrailListElement<T> current = head;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        insertList(current, c);
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void clear() {
        TrailListElement<T> first = head.next;
        TrailListElement<T> last = tail.prev;

        head.next = tail;
        tail.prev = head;

        trail.pushState(new StateEntry() {
            @Override
            public void restore() {
                head.next = first;
                tail.prev = last;
            }
        });

        size.setValue(0);
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size.value()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size.value());
        }
        TrailListElement<T> current = head.next;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        return current.getValue();
    }

    @Override
    public T set(int index, T element) {
        if (index < 0 || index >= size.value()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size.value());
        }
        TrailListElement<T> current = head.next;
        for (int i = 0; i < index; i++) {
            current = current.next;
        }
        T oldValue = current.getValue();
        current.setValue(element);
        TrailListElement<T> finalCurrent = current;
        trail.pushState(new StateEntry() {
            @Override
            public void restore() {
                finalCurrent.setValue(oldValue);
            }
        });
        return oldValue;
    }

    @Override
    public void add(int index, T element) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public T remove(int index) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Not implemented yet");
    }


    public boolean remove(Object value) {
        TrailListElement<T> current = head.next;
        int i = 0;
        while (i < size.value()) {
            // on parcourt la liste
            if (current.getValue() == value) {
                // on a trouvé l'élément à supprimer
                current.prev.next = current.next;
                current.next.prev = current.prev;
                TrailListElement<T> finalCurrent = current;
                trail.pushState(new StateEntry() {

                    @Override
                    public void restore() {
                        // on remet l'élément à sa place
                        finalCurrent.prev.next = finalCurrent;
                        finalCurrent.next.prev = finalCurrent;

                    }
                });
                size.setValue(size.value() - 1);

                return true;
            }
            current = current.next;
            i++;
        }
        return false;
    }
}
