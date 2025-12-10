package org.maxicp.state.datastructures;

import org.maxicp.state.StateManager;
import java.util.function.Function;

public class StateDoubleLinkedListMDD<T> extends StateDoubleLinkedList<T> {

    private final Function<T, StateDoubleLinkedListNode<T>> handleOf;
    public StateDoubleLinkedListMDD(StateManager sm, Function<T, StateDoubleLinkedListNode<T>> handleOf) {
        super(sm);
        this.handleOf = handleOf;
    }

    @Override
    protected StateDoubleLinkedListNode<T> getNode(T t) {
        return handleOf.apply(t);
    }

    @Override
    public boolean remove(Object o) {
        if (o == null) return false;
        T element = (T) o;
        StateDoubleLinkedListNode<T> current = handleOf.apply(element);
        if (current.previous.value() == null) // check for deactivated end
            return false;
        StateDoubleLinkedListNode<T> previous = current.previous.value();
        StateDoubleLinkedListNode<T> next = current.next.value();
        previous.next.setValue(next);
        next.previous.setValue(previous);
        current.previous.setValue(null); // deactivate one end so we know it is out
        size.decrement();
        return true;
    }


}
