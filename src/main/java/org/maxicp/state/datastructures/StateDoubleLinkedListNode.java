package org.maxicp.state.datastructures;

import org.maxicp.state.State;
import org.maxicp.state.StateManager;

// Cell of a Reversible DoubleLinkedList
public class StateDoubleLinkedListNode<T> {
    public State<StateDoubleLinkedListNode<T>> previous;
    public State<StateDoubleLinkedListNode<T>> next;
    public T element;

    public StateDoubleLinkedListNode(StateManager sm, T element) {
        this.previous = sm.makeStateRef(null);
        this.next = sm.makeStateRef(null);
        this.element = element;
    }

    @Override
    public String toString() {
        return element.toString();
    }
}